package com.example.hybridai.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

/**
 * Manages background downloads of GGUF model files using Android's DownloadManager.
 * Downloads survive app minimisation and device sleep.
 */
class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
    }

    // Progress: modelId → 0.0f to 1.0f (-1 = error, 1.0 = done, 0.999f = verifying)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

    private val activeDownloads = mutableMapOf<Long, ModelInfo>() // downloadId → ModelInfo
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Start downloading a model. Saves to app's external files directory.
     */
    fun startDownload(model: ModelInfo) {
        Log.d(TAG, "Starting download for ${model.name}")

        val destFile = getModelFile(model.filename)
        if (destFile.exists()) {
            Log.d(TAG, "File already exists: ${destFile.absolutePath}")
            updateProgress(model.id, 1.0f)
            return
        }

        val request = DownloadManager.Request(Uri.parse(model.downloadUrl)).apply {
            setTitle("Downloading ${model.name}")
            setDescription("Local AI model — ${model.sizeLabel}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, model.filename)
            // Force binary download — prevents HuggingFace redirecting to HTML login page
            setMimeType("application/octet-stream")
            addRequestHeader("User-Agent", "Mozilla/5.0 Android")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        }

        val downloadId = downloadManager.enqueue(request)
        activeDownloads[downloadId] = model
        updateProgress(model.id, 0.01f) // Show immediately that download started
    }

    /**
     * Poll DownloadManager for progress updates. Call this periodically from ViewModel.
     */
    fun checkProgress() {
        val idsToRemove = mutableListOf<Long>()

        for ((downloadId, model) in activeDownloads) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val progress = if (total > 0) downloaded.toFloat() / total else 0.05f
                            updateProgress(model.id, progress.coerceIn(0.01f, 0.99f))
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            idsToRemove.add(downloadId)
                            updateProgress(model.id, 0.999f) // Verifying state
                            
                            scope.launch {
                                // Validate it's actually a GGUF file (not HTML error page / ZIP)
                                val file = getModelFile(model.filename)
                                val isGguf = try {
                                    file.inputStream().use { s ->
                                        val bytes = ByteArray(4)
                                        s.read(bytes)
                                        String(bytes) == "GGUF"
                                    }
                                } catch (e: Exception) { false }

                                if (!isGguf) {
                                    file.delete() // Remove the bad file
                                    updateProgress(model.id, -1f)
                                    Log.e(TAG, "❌ Downloaded file is NOT a GGUF (got HTML or ZIP). Deleted. Check URL.")
                                    return@launch
                                }
                                
                                // Verify SHA256 Checksum if provided
                                if (model.sha256.isNotEmpty()) {
                                    Log.i(TAG, "Verifying SHA256 checksum for ${model.name}...")
                                    try {
                                        val digest = MessageDigest.getInstance("SHA-256")
                                        file.inputStream().use { fis ->
                                            val buffer = ByteArray(8192)
                                            var bytesRead: Int
                                            while (fis.read(buffer).also { bytesRead = it } != -1) {
                                                digest.update(buffer, 0, bytesRead)
                                            }
                                        }
                                        val hash = digest.digest().joinToString("") { "%02x".format(it) }
                                        if (hash.equals(model.sha256, ignoreCase = true)) {
                                            Log.i(TAG, "✅ Checksum matched for ${model.name}")
                                            updateProgress(model.id, 1.0f)
                                        } else {
                                            Log.e(TAG, "❌ Checksum mismatch for ${model.name}. Expected ${model.sha256}, got $hash")
                                            file.delete()
                                            updateProgress(model.id, -1f)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error calculating checksum", e)
                                        updateProgress(model.id, -1f)
                                    }
                                } else {
                                    updateProgress(model.id, 1.0f)
                                    Log.i(TAG, "✅ Valid GGUF downloaded (No Checksum): ${model.name} (${file.length() / 1_000_000}MB)")
                                }
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            updateProgress(model.id, -1f)
                            idsToRemove.add(downloadId)
                            Log.e(TAG, "Download failed: ${model.name}")
                        }
                    }
                }
            }
        }
        idsToRemove.forEach { activeDownloads.remove(it) }
    }

    /**
     * Returns the File path where a model will be saved.
     */
    fun getModelFile(filename: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(dir, filename)
    }

    /**
     * Returns true if the model file exists on disk.
     */
    fun isModelDownloaded(model: ModelInfo): Boolean {
        return getModelFile(model.filename).exists()
    }

    /**
     * Cancel an active download.
     */
    fun cancelDownload(model: ModelInfo) {
        val downloadId = activeDownloads.entries.firstOrNull { it.value.id == model.id }?.key
        if (downloadId != null) {
            downloadManager.remove(downloadId)
            activeDownloads.remove(downloadId)
        }
        val file = getModelFile(model.filename)
        if (file.exists()) { file.delete() }
        
        val current = _downloadProgress.value.toMutableMap()
        current.remove(model.id)
        _downloadProgress.value = current
    }

    /**
     * Delete a model file from disk.
     */
    fun deleteModel(model: ModelInfo) {
        getModelFile(model.filename).let { file ->
            if (file.exists()) file.delete()
        }
        val current = _downloadProgress.value.toMutableMap()
        current.remove(model.id)
        _downloadProgress.value = current
    }

    private fun updateProgress(modelId: String, progress: Float) {
        val current = _downloadProgress.value.toMutableMap()
        current[modelId] = progress
        _downloadProgress.value = current
    }
}
