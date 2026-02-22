package com.example.hybridai.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real llama.cpp inference engine via JNI.
 * Loads a GGUF model file and generates responses token-by-token on device.
 * System prompt adapted for a mobile assistant on low-RAM phones.
 */
class LlamaCppEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"

        private const val SYSTEM_PROMPT =
            "You are a helpful AI assistant running locally on an Android phone. " +
            "Keep responses concise — under 3 sentences for simple questions. " +
            "You have no internet access."

        // Temperature: 0.8 (creative but coherent)
        // minP: 0.05 (filters low-probability tokens)
        // Context: 2048 tokens (good balance for 4GB RAM devices)
        private const val TEMPERATURE   = 0.8f
        private const val MIN_P         = 0.05f
        private const val CONTEXT_SIZE  = 2048L
        private const val NUM_THREADS   = 4

        init {
            System.loadLibrary("hybridai")
        }
    }

    override val engineName: String = "llama.cpp (GGUF)"

    private var nativePtr: Long = 0L
    private var loadedModelName: String = ""

    /**
     * Loads the GGUF model into memory via JNI.
     * Uses mmap so large models don't consume all RAM upfront.
     */
    override suspend fun loadModel(modelPath: String, useMmap: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(modelPath)
            if (!file.exists()) {
                Log.w(TAG, "Model file not found: $modelPath")
                return@withContext false
            }

            // Validate GGUF magic bytes before passing to C++
            val isGguf = try {
                file.inputStream().use { s ->
                    val h = ByteArray(4); s.read(h); String(h) == "GGUF"
                }
            } catch (e: Exception) { false }

            if (!isGguf) {
                Log.e(TAG, "Not a valid GGUF file: $modelPath")
                return@withContext false
            }

            try {
                nativePtr = loadModelJNI(
                    modelPath  = modelPath,
                    minP       = MIN_P,
                    temperature= TEMPERATURE,
                    storeChats = true,
                    contextSize= CONTEXT_SIZE,
                    chatTemplate = "",   // use template embedded in the GGUF
                    nThreads   = NUM_THREADS,
                    useMmap    = useMmap,
                    useMlock   = false
                )
                if (nativePtr != 0L) {
                    loadedModelName = file.name
                    // Add the system prompt so the model knows its role
                    addChatMessageJNI(nativePtr, SYSTEM_PROMPT, "system")
                    Log.i(TAG, "✅ Loaded: $loadedModelName (ptr=$nativePtr)")
                    true
                } else {
                    Log.e(TAG, "loadModelJNI returned null pointer")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "JNI loadModel failed: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Streams real token-by-token inference as a Flow<String>.
     * Each emission is a token piece (word fragment) from the model.
     */
    override fun generateResponse(prompt: String): Flow<String> = flow {
        if (nativePtr == 0L) {
            emit("🟡 No local model loaded.\n\n")
            emit("Go to ⚙️ Settings → 🧠 Local Models → download a model → tap 'Use this model'.")
            return@flow
        }

        try {
            Log.d(TAG, "Starting inference for: ${prompt.take(50)}")
            withContext(Dispatchers.IO) {
                startCompletionJNI(nativePtr, prompt)
            }

            var tokenCount = 0
            while (true) {
                val piece = withContext(Dispatchers.IO) {
                    completionLoopJNI(nativePtr)
                }
                if (piece == "[EOG]") {
                    Log.d(TAG, "Inference complete. Tokens: $tokenCount")
                    break
                }
                if (piece.isNotEmpty()) {
                    emit(piece)
                    tokenCount++
                }
            }

            withContext(Dispatchers.IO) {
                stopCompletionJNI(nativePtr)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}", e)
            emit("\n⚠️ Inference error: ${e.message}")
        }
    }

    override fun unload() {
        if (nativePtr != 0L) {
            closeModelJNI(nativePtr)
            nativePtr = 0L
            loadedModelName = ""
            Log.i(TAG, "Model unloaded")
        }
    }

    // ── JNI declarations (implemented in hybridai.cpp) ────────────────────

    private external fun loadModelJNI(
        modelPath: String,
        minP: Float,
        temperature: Float,
        storeChats: Boolean,
        contextSize: Long,
        chatTemplate: String,
        nThreads: Int,
        useMmap: Boolean,
        useMlock: Boolean
    ): Long

    private external fun addChatMessageJNI(modelPtr: Long, message: String, role: String)

    private external fun startCompletionJNI(modelPtr: Long, prompt: String)

    private external fun completionLoopJNI(modelPtr: Long): String

    private external fun stopCompletionJNI(modelPtr: Long)

    private external fun closeModelJNI(modelPtr: Long)

    private external fun getSpeedJNI(modelPtr: Long): Float
}
