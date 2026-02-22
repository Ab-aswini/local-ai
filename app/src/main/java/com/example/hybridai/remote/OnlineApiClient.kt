package com.example.hybridai.remote

import android.util.Log
import com.example.hybridai.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini 1.5 Flash API client.
 * Reads the API key from AppPreferences (DataStore) at call time —
 * so users can update their key in Settings without restarting the app.
 */
class OnlineApiClient(private val prefs: AppPreferences) {

    companion object {
        private const val TAG = "OnlineApiClient"
        private const val MODEL = "gemini-1.5-flash"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    }

    fun generateResponse(prompt: String): Flow<String> = flow {
        val apiKey = prefs.geminiApiKey.first()

        if (apiKey.isBlank()) {
            emit("☁️ No API key set. Go to Settings → Cloud AI tab → paste your free Gemini key.")
            return@flow
        }

        Log.d(TAG, "Sending to Gemini: ${prompt.take(50)}...")

        try {
            val response = withContext(Dispatchers.IO) { callGeminiApi(prompt, apiKey) }
            for (word in response.split(" ")) {
                emit("$word ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini error: ${e.message}", e)
            emit("⚠️ Error connecting to Gemini: ${e.message}")
        }
    }

    private fun callGeminiApi(prompt: String, apiKey: String): String {
        val url = URL("$BASE_URL?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

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

            OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()); it.flush() }

            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                parseGeminiResponse(connection.inputStream.bufferedReader().readText())
            } else {
                val err = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "HTTP $code: $err")
                "API Error ($code) — check your API key in Settings."
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseGeminiResponse(json: String): String {
        return try {
            JSONObject(json)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            "Could not parse Gemini response."
        }
    }
}
