package com.example.hybridai.core

import android.util.Log
import com.example.hybridai.data.PersonaCatalog
import kotlinx.coroutines.flow.Flow

enum class TaskComplexity { SIMPLE, COMPLEX }

/**
 * Routes user prompts to local (llama.cpp) or cloud (Gemini) based on complexity.
 * Injects the selected persona's system prompt ahead of generation.
 * Exposes [lastUsedCloud] so the ViewModel can tag message role correctly.
 */
class TaskOrchestrator(
    private val localInferenceManager: com.example.hybridai.local.LocalInferenceManager,
    private val onlineApiClient: com.example.hybridai.remote.OnlineApiClient
) {
    companion object {
        private const val TAG = "TaskOrchestrator"

        private val COMPLEX_KEYWORDS = listOf(
            "code", "write a script", "analyze", "deep dive", "research",
            "explain", "compare", "summarize", "translate", "debug", "refactor",
            "what is", "how does", "why does", "difference between", "essay",
            "write a", "generate a report"
        )
    }

    /** What persona to use for this session. Defaults to Assistant. */
    var activePersonaId: String = "assistant"

    /** True after the last call if cloud was used */
    var lastUsedCloud: Boolean = false
        private set

    /** Legacy alias */
    fun processPrompt(prompt: String) = processInput(prompt)

    /**
     * Evaluates complexity and routes to local or cloud.
     * Prepends the persona system prompt to the user's message.
     */
    fun processInput(prompt: String): Flow<String> {
        val persona = PersonaCatalog.findById(activePersonaId)
        val augmentedPrompt = buildPromptWithPersona(persona.systemPrompt, prompt)
        val complexity = evaluateComplexity(prompt)

        return if (complexity == TaskComplexity.SIMPLE) {
            Log.i(TAG, "→ LOCAL [${persona.name}]: $prompt")
            lastUsedCloud = false
            localInferenceManager.generateResponse(augmentedPrompt)
        } else {
            Log.i(TAG, "→ CLOUD [${persona.name}]: $prompt")
            lastUsedCloud = true
            onlineApiClient.generateResponse(augmentedPrompt)
        }
    }

    private fun buildPromptWithPersona(systemPrompt: String, userPrompt: String): String {
        // Local model uses raw prompt — system prompt already set at model load.
        // For cloud (Gemini), we prepend it inline since Gemini doesn't support
        // a separate system role via simple HTTP without chat history.
        return "[SYSTEM: $systemPrompt]\n\nUSER: $userPrompt"
    }

    private fun evaluateComplexity(prompt: String): TaskComplexity {
        val lower = prompt.lowercase()
        if (prompt.length > 200) return TaskComplexity.COMPLEX
        if (COMPLEX_KEYWORDS.any { lower.contains(it) }) return TaskComplexity.COMPLEX
        return TaskComplexity.SIMPLE
    }
}
