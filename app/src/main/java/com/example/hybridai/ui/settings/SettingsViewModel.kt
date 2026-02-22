package com.example.hybridai.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hybridai.data.AppPreferences
import com.example.hybridai.data.ModelCatalog
import com.example.hybridai.data.ModelDownloadManager
import com.example.hybridai.data.ModelInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    val downloadManager = ModelDownloadManager(application)

    // ── Flows from DataStore ───────────────────────
    val geminiApiKey: StateFlow<String> = prefs.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedModelName: StateFlow<String> = prefs.selectedModelName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedModelPath: StateFlow<String> = prefs.selectedModelPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val appTheme: StateFlow<String> = prefs.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "amoled")

    val selectedPersonaId: StateFlow<String> = prefs.selectedPersonaId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "assistant")

    // Download progress map
    val downloadProgress: StateFlow<Map<String, Float>> = downloadManager.downloadProgress

    /**
     * Poll download progress every second while viewmodel is active.
     */
    init {
        viewModelScope.launch {
            while (true) {
                downloadManager.checkProgress()
                delay(1000)
            }
        }

        // When model finishes downloading → auto-save path to prefs
        viewModelScope.launch {
            downloadProgress.collect { progressMap ->
                for (model in ModelCatalog.models) {
                    if (progressMap[model.id] == 1.0f) {
                        val file = downloadManager.getModelFile(model.filename)
                        if (file.exists() && selectedModelPath.value != file.absolutePath) {
                            prefs.saveSelectedModel(file.absolutePath, model.name)
                        }
                    }
                }
            }
        }
    }

    // ── Actions ───────────────────────────────────

    fun saveApiKey(key: String) {
        viewModelScope.launch { prefs.saveGeminiApiKey(key.trim()) }
    }

    fun downloadModel(model: ModelInfo) {
        downloadManager.startDownload(model)
    }

    fun deleteModel(model: ModelInfo) {
        viewModelScope.launch {
            downloadManager.deleteModel(model)
            if (selectedModelName.value == model.name) {
                prefs.clearModel()
            }
        }
    }

    fun activateModel(model: ModelInfo) {
        viewModelScope.launch {
            val file = downloadManager.getModelFile(model.filename)
            if (file.exists()) {
                prefs.saveSelectedModel(file.absolutePath, model.name)
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefs.saveTheme(theme) }
    }

    fun savePersona(personaId: String) {
        viewModelScope.launch { prefs.savePersona(personaId) }
    }

    fun isDownloaded(model: ModelInfo): Boolean {
        return downloadManager.isModelDownloaded(model)
    }
}
