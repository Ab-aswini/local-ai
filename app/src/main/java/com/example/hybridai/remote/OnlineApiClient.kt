package com.example.hybridai.remote

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Connects to remote Cloud endpoints like OpenAI or Gemini for complex queries that
 * the local engines cannot handle well.
 */
class OnlineApiClient {

    companion object {
        private const val TAG = "OnlineApiClient"
        private const val MOCK_DELAY_MS = 100L
    }

    // In a production app, use Ktor HttpClient or Retrofit here.

    /**
     * Sends the prompt over network and streams back the result.
     */
    fun generateResponse(prompt: String): Flow<String> = flow {
        Log.d(TAG, "Executing network request for complex prompt.")
        
        // Simulating network latency
        delay(800)
        
        val mockResponseTokens = listOf(
            "Here ", "is ", "the ", "advanced ", "analysis ", "from ", "the ", "cloud ", "API. ", "It ", "handles ", "code ", "well."
        )
        
        for (token in mockResponseTokens) {
            delay(10) // Simulate streaming text chunks
            emit(token)
        }
    }
}
