package com.example.hybridai.local

import android.content.Context
import android.util.Log
import com.example.hybridai.core.InferenceStrategy
import com.example.hybridai.core.SystemHealthMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Abstraction layer coordinating between different local engines.
 */
class LocalInferenceManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalInferenceManager"
    }

    private val healthMonitor = SystemHealthMonitor(context)
    private var activeEngine: InferenceEngine? = null

    // For demonstration, we assume paths. In reality, these are downloaded or shipped in assets.
    private val llamaGgufPath = "/data/local/tmp/llama-3.2-1b-instruct-q4_k_m.gguf"
    private val gemmaTaskPath = "/data/local/tmp/gemma-3-1b.task"

    suspend fun initialize() {
        val strategy = healthMonitor.determineOptimalStrategy()
        
        // Choose engine based on some configuration or strategy. 
        // Here we demonstrate preferring LlamaCpp for memory mapping on low ram.
        activeEngine = if (strategy == InferenceStrategy.LOCAL_QUANTIZED_4BIT) {
            Log.i(TAG, "Initializing LlamaCppEngine (.gguf) for low RAM footprint.")
            LlamaCppEngine().apply {
                loadModel(llamaGgufPath, useMmap = true)
            }
        } else {
            Log.i(TAG, "Initializing MediaPipeEngine (.task) for optimized hardware paths.")
            MediaPipeEngine(context).apply {
                loadModel(gemmaTaskPath, useMmap = false)
            }
        }
    }

    fun generateResponse(prompt: String): Flow<String> {
        val engine = activeEngine
        if (engine == null) {
            Log.e(TAG, "LocalInferenceManager is not initialized!")
            return flow { emit("Error: Local Engine not initialized.") }
        }
        return engine.generateResponse(prompt)
    }

    fun shutdown() {
        activeEngine?.unload()
        activeEngine = null
    }
}
