package com.example.hybridai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

// Top-level extension to create a single DataStore instance per app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Central store for all user preferences.
 * Backed by Jetpack DataStore — survives app restarts.
 */
class AppPreferences(private val context: Context) {

    companion object {
        val GEMINI_API_KEY       = stringPreferencesKey("gemini_api_key")
        val SELECTED_MODEL_PATH  = stringPreferencesKey("selected_model_path")
        val SELECTED_MODEL_NAME  = stringPreferencesKey("selected_model_name")
        val APP_THEME            = stringPreferencesKey("app_theme")    // "dark" | "amoled" | "light"
        val SELECTED_PERSONA_ID  = stringPreferencesKey("persona_id")   // see PersonaCatalog
        val HAS_SEEN_ONBOARDING  = booleanPreferencesKey("onboarding")   // first launch flag

        // Inference Controls
        val INFERENCE_TEMP         = floatPreferencesKey("inference_temp")
        val INFERENCE_CONTEXT_SIZE = intPreferencesKey("inference_ctx_size")
        val INFERENCE_MAX_TOKENS   = intPreferencesKey("inference_max_tokens")

        // Voice Controls
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_SPEED   = floatPreferencesKey("tts_speed")
    }

    // ── Reads ──────────────────────────────────────

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_API_KEY] ?: ""
    }

    val selectedModelPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_MODEL_PATH] ?: ""
    }

    val selectedModelName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_MODEL_NAME] ?: ""
    }

    val appTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_THEME] ?: "amoled"
    }

    val selectedPersonaId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_PERSONA_ID] ?: "assistant"
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING] ?: false
    }

    fun getModelPerformance(modelName: String): Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[floatPreferencesKey("perf_$modelName")] ?: 0f
    }

    val inferenceTemperature: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[INFERENCE_TEMP] ?: 0.8f
    }

    val inferenceContextSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[INFERENCE_CONTEXT_SIZE] ?: 2048
    }

    val inferenceMaxTokens: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[INFERENCE_MAX_TOKENS] ?: -1 // -1 implies max/maximum
    }

    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TTS_ENABLED] ?: false
    }

    val ttsSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[TTS_SPEED] ?: 1.0f
    }

    // ── Writes ─────────────────────────────────────

    suspend fun saveGeminiApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[GEMINI_API_KEY] = key }
    }

    suspend fun saveSelectedModel(path: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL_PATH] = path
            prefs[SELECTED_MODEL_NAME] = name
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[APP_THEME] = theme }
    }

    suspend fun savePersona(personaId: String) {
        context.dataStore.edit { prefs -> prefs[SELECTED_PERSONA_ID] = personaId }
    }

    suspend fun markOnboardingSeen() {
        context.dataStore.edit { prefs -> prefs[HAS_SEEN_ONBOARDING] = true }
    }

    suspend fun clearModel() {
        context.dataStore.edit { prefs ->
            prefs.remove(SELECTED_MODEL_PATH)
            prefs.remove(SELECTED_MODEL_NAME)
        }
    }

    suspend fun saveModelPerformance(modelName: String, tokensPerSec: Float) {
        context.dataStore.edit { prefs ->
            prefs[floatPreferencesKey("perf_$modelName")] = tokensPerSec
        }
    }

    suspend fun saveInferenceTemperature(temp: Float) {
        context.dataStore.edit { prefs -> prefs[INFERENCE_TEMP] = temp }
    }

    suspend fun saveInferenceContextSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[INFERENCE_CONTEXT_SIZE] = size }
    }

    suspend fun saveInferenceMaxTokens(tokens: Int) {
        context.dataStore.edit { prefs -> prefs[INFERENCE_MAX_TOKENS] = tokens }
    }

    suspend fun saveTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[TTS_ENABLED] = enabled }
    }

    suspend fun saveTtsSpeed(speed: Float) {
        context.dataStore.edit { prefs -> prefs[TTS_SPEED] = speed }
    }
}
