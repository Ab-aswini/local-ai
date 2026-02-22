package com.example.hybridai.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real Gemini API client.
 * Connects to Google's Gemini 1.5 Flash (free tier) for complex queries.
 *
 * HOW TO GET YOUR API KEY (free):
 * 1. Go to https://aistudio.google.com/app/apikey
 * 2. Click "Create API Key"
 * 3. Paste the key below as the value of GEMINI_API_KEY
 */
class OnlineApiClient {

    companion object {
        private const val TAG = "OnlineApiClient"

        // ⬇️ PASTE YOUR FREE GEMINI API KEY HERE ⬇️
        private const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"

        private const val MODEL = "gemini-1.5-flash"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    }

    /**
     * Sends prompt to Gemini API and streams back the response.
     * Falls back to an error message gracefully if no API key is set.
     */
    fun generateResponse(prompt: String): Flow<String> = flow {
        // Guard: if API key not configured yet, emit a helpful message
        if (GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            emit("⚠️ No API key set. Go to aistudio.google.com → Get API Key → paste it in OnlineApiClient.kt")
            return@flow
        }

        Log.d(TAG, "Sending to Gemini API: ${prompt.take(50)}...")

        try {
            val response = withContext(Dispatchers.IO) {
                callGeminiApi(prompt)
            }
            // Emit the response word by word for a streaming feel
            val words = response.split(" ")
            for (word in words) {
                emit("$word ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error: ${e.message}", e)
            emit("Error connecting to Gemini: ${e.message}")
        }
    }

    private fun callGeminiApi(prompt: String): String {
        val url = URL("$BASE_URL?key=$GEMINI_API_KEY")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            // Build the Gemini API request body
            val systemInstruction = "You are a helpful, concise AI assistant. Keep responses under 3 sentences when possible."
            val requestBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 512)
                })
            }

            // Send the request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().readText()
                parseGeminiResponse(responseText)
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "HTTP $responseCode: $errorText")
                "API Error ($responseCode). Check your API key."
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseGeminiResponse(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: ${e.message}")
            "Could not parse response from Gemini."
        }
    }
}
