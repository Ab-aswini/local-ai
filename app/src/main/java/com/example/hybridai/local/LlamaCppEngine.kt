package com.example.hybridai.local

import android.content.Context
import android.util.Log
import com.example.hybridai.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper for llama.cpp JNI bindings.
 * Currently uses smart mock responses until real llama.cpp C++ is integrated.
 * When a GGUF model is downloaded and set in preferences, it acknowledges the model.
 */
class LlamaCppEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"
    }

    override val engineName: String = "LlamaCpp / GGUF"
    private var loadedModelPath: String = ""
    private var loadedModelName: String = ""
    private var isLoaded = false

    /**
     * Loads a GGUF model from disk.
     * Real implementation will call JNI: nativeContextId = loadGgufModelJNI(modelPath, useMmap)
     */
    override suspend fun loadModel(modelPath: String, useMmap: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(modelPath)
            if (!file.exists()) {
                Log.w(TAG, "Model file not found: $modelPath")
                isLoaded = false
                return@withContext false
            }

            // Validate it's actually a GGUF file (starts with "GGUF" magic bytes)
            val isGguf = try {
                file.inputStream().use { stream ->
                    val header = ByteArray(4)
                    stream.read(header)
                    String(header) == "GGUF"
                }
            } catch (e: Exception) {
                false
            }

            if (!isGguf) {
                Log.e(TAG, "File is not a valid GGUF: $modelPath")
                isLoaded = false
                return@withContext false
            }

            // Real call would be: nativeContextId = loadGgufModelJNI(modelPath, useMmap)
            loadedModelPath = modelPath
            loadedModelName = file.name
            isLoaded = true
            Log.i(TAG, "Model ready (stub): $loadedModelName (${file.length() / 1_000_000}MB)")
            true
        }
    }

    override fun generateResponse(prompt: String): Flow<String> = flow {
        if (!isLoaded) {
            emit("🟡 No local model loaded yet.\n\n")
            emit("Go to ⚙️ Settings → 🧠 Local Models → download a model → tap 'Use this model'.\n\n")
            emit("Or use Cloud AI (Settings → 🔑 Cloud AI) for real AI responses now.")
            return@flow
        }

        // Model is downloaded and verified — but real inference needs llama.cpp C++ (Phase 3)
        // For now, respond with context-aware stubs that are honest about the state
        val response = when {
            prompt.contains("hello", ignoreCase = true) || prompt.contains("hi", ignoreCase = true) ->
                "Hello! I'm using $loadedModelName on your device. Real llama.cpp inference is coming in the next update!"

            prompt.contains("what", ignoreCase = true) && prompt.contains("you", ignoreCase = true) ->
                "I'm a local AI assistant. My model ($loadedModelName) is loaded and ready. Full on-device inference will be active after the llama.cpp C++ integration."

            prompt.contains("model", ignoreCase = true) || prompt.contains("downloaded", ignoreCase = true) ->
                "✅ $loadedModelName is downloaded and verified on your device! Real inference is the next step."

            prompt.length < 30 ->
                "I received your message using $loadedModelName locally. Real token generation is coming soon!"

            else ->
                "Processing with $loadedModelName (on-device). Note: Real llama.cpp C++ inference is Phase 3 of this project. Right now I'm running a stub that acknowledges the model."
        }

        for (word in response.split(" ")) {
            delay(30)
            emit("$word ")
        }
    }

    override fun unload() {
        // Real: if (nativeContextId != 0L) freeGgufModelJNI(nativeContextId)
        isLoaded = false
        loadedModelPath = ""
        loadedModelName = ""
    }
}
