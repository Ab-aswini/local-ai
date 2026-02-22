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
import java.io.File

/**
 * Manages background downloads of GGUF model files using Android's DownloadManager.
 * Downloads survive app minimisation and device sleep.
 */
class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
    }

    // Progress: modelId → 0.0f to 1.0f (-1 = error, 1.0 = done)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

    private val activeDownloads = mutableMapOf<Long, ModelInfo>() // downloadId → ModelInfo
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

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
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
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
                            updateProgress(model.id, 1.0f)
                            idsToRemove.add(downloadId)
                            Log.d(TAG, "Download complete: ${model.name}")
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
