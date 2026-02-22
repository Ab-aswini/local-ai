# Add project specific ProGuard rules here.

# ── JNI / Native Classes ─────────────────────────────────────────────────────
# Keep LlamaCppEngine and all its external (JNI) functions. Without this,
# Release builds strip the native method names and the JNI bridge breaks.
-keep class com.example.hybridai.local.LlamaCppEngine {
    native <methods>;
    *;
}
-keep class com.example.hybridai.local.LocalInferenceManager { *; }
-keep class com.example.hybridai.local.InferenceEngine { *; }

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class com.example.hybridai.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}

# ── DataStore ────────────────────────────────────────────────────────────────
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences$Key { *; }

# ── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Gemini / Generative AI SDK ───────────────────────────────────────────────
-keep class com.google.ai.client.generativeai.** { *; }

# ── Suppress noisy warnings ──────────────────────────────────────────────────
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn org.slf4j.**
