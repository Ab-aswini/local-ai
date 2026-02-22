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
}
