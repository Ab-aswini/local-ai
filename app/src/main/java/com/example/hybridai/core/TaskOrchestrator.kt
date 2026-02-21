package com.example.hybridai.core

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class TaskComplexity {
    SIMPLE,  // e.g., small chat, system commands
    COMPLEX  // e.g., deep analysis, coding help
}

/**
 * The 'Hybrid Router'. Analyzes user intent and routes to Local or Online engines.
 */
class TaskOrchestrator(
    private val localInferenceManager: com.example.hybridai.local.LocalInferenceManager,
    private val onlineApiClient: com.example.hybridai.remote.OnlineApiClient
) {
    companion object {
        private const val TAG = "TaskOrchestrator"
        
        // Basic heuristic for complexity. In a real app, this could use a small local classifier
        // or regex patterns for specific intents.
        private val COMPLEX_KEYWORDS = listOf("code", "write a script", "analyze", "deep dive", "research")
    }

    /**
     * Evaluates user prompt and routes to the appropriate engine.
     * Returns a Flow of strings representing the streamed response.
     */
    fun processPrompt(prompt: String): Flow<String> {
        val complexity = evaluateComplexity(prompt)
        
        return if (complexity == TaskComplexity.SIMPLE) {
            Log.i(TAG, "Routing to Local Engine for SIMPLE task.")
            localInferenceManager.generateResponse(prompt)
        } else {
            Log.i(TAG, "Routing to Online API for COMPLEX task.")
            onlineApiClient.generateResponse(prompt)
        }
    }

    private fun evaluateComplexity(prompt: String): TaskComplexity {
        val lowerPrompt = prompt.lowercase()
        // If the prompt is very long, it likely requires more context/power
        if (prompt.length > 500) {
            return TaskComplexity.COMPLEX
        }
        
        // Checking for coding or complex keywords
        if (COMPLEX_KEYWORDS.any { lowerPrompt.contains(it) }) {
            return TaskComplexity.COMPLEX
        }
        
        // Default to simple/local
        return TaskComplexity.SIMPLE
    }
}
