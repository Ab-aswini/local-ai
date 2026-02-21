package com.example.hybridai.local

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for both MediaPipe (.task) and llama.cpp (.gguf) engines.
 */
interface InferenceEngine {
    val engineName: String
    suspend fun loadModel(modelPath: String, useMmap: Boolean = true): Boolean
    fun generateResponse(prompt: String): Flow<String>
    fun unload()
}
