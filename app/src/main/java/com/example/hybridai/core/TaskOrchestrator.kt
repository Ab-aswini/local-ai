package com.example.hybridai.core

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

enum class TaskComplexity { SIMPLE, COMPLEX }

/**
 * Routes user prompts to local (llama.cpp) or cloud (Gemini) based on complexity.
 * Exposes `lastUsedCloud` so the ViewModel can correctly tag the message role.
 */
class TaskOrchestrator(
    private val localInferenceManager: com.example.hybridai.local.LocalInferenceManager,
    private val onlineApiClient: com.example.hybridai.remote.OnlineApiClient
) {
    companion object {
        private const val TAG = "TaskOrchestrator"

        // Heuristics — extends to an ML classifier in future
        private val COMPLEX_KEYWORDS = listOf(
            "code", "write a script", "analyze", "deep dive", "research",
            "explain", "compare", "summarize", "translate", "debug", "refactor",
            "what is", "how does", "why does", "difference between"
        )
    }

    /** True if the last routed call used the cloud (Gemini) engine */
    var lastUsedCloud: Boolean = false
        private set

    /**
     * Evaluates complexity and routes to local or cloud engine.
     * Returns a Flow<String> of streamed token pieces.
     */
    fun processInput(prompt: String): Flow<String> {
        val complexity = evaluateComplexity(prompt)

        return if (complexity == TaskComplexity.SIMPLE) {
            Log.i(TAG, "→ LOCAL: simple task")
            lastUsedCloud = false
            localInferenceManager.generateResponse(prompt)
        } else {
            Log.i(TAG, "→ CLOUD: complex task")
            lastUsedCloud = true
            onlineApiClient.generateResponse(prompt)
        }
    }

    /** Legacy alias */
    fun processPrompt(prompt: String) = processInput(prompt)

    private fun evaluateComplexity(prompt: String): TaskComplexity {
        val lower = prompt.lowercase()
        if (prompt.length > 200) return TaskComplexity.COMPLEX
        if (COMPLEX_KEYWORDS.any { lower.contains(it) }) return TaskComplexity.COMPLEX
        return TaskComplexity.SIMPLE
    }
}
