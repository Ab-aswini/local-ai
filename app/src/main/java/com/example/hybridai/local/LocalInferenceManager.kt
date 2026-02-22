package com.example.hybridai.local

import android.content.Context
import android.util.Log
import com.example.hybridai.data.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Coordinates local inference engines.
 * Loads the user's selected GGUF model from DataStore preferences automatically.
 */
class LocalInferenceManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalInferenceManager"
    }

    private val prefs = AppPreferences(context)
    private val llamaEngine = LlamaCppEngine(context)
    private var initialized = false

    /**
     * Called on app start. Reads the saved model path from DataStore and loads it.
     * If no model is selected yet, engine is ready but will show guidance when queried.
     */
    suspend fun initialize() {
        val modelPath = prefs.selectedModelPath.first()
        val modelName = prefs.selectedModelName.first()

        if (modelPath.isNotBlank()) {
            Log.i(TAG, "Loading saved model: $modelName from $modelPath")
            val loaded = llamaEngine.loadModel(modelPath, useMmap = true)
            if (loaded) {
                Log.i(TAG, "✅ Model loaded successfully: $modelName")
            } else {
                Log.w(TAG, "⚠️ Model file invalid or missing: $modelPath — clearing saved path")
                prefs.clearModel()
            }
        } else {
            Log.i(TAG, "No local model selected yet. User can download one in Settings.")
        }
        initialized = true
    }

    /**
     * Called after a model is downloaded and activated — reloads it immediately.
     */
    suspend fun reloadModel() {
        llamaEngine.unload()
        initialize()
    }

    fun generateResponse(prompt: String): Flow<String> {
        if (!initialized) {
            return flow { emit("Local engine starting up, please try again in a moment.") }
        }
        return llamaEngine.generateResponse(prompt)
    }

    fun shutdown() {
        llamaEngine.unload()
        initialized = false
    }
}
