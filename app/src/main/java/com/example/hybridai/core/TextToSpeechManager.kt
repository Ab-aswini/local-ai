package com.example.hybridai.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Tracks if we are currently speaking something
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // ID of the message currently being spoken
    private val _currentlySpeakingMessageId = MutableStateFlow<Long?>(null)
    val currentlySpeakingMessageId: StateFlow<Long?> = _currentlySpeakingMessageId.asStateFlow()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Language not supported or missing data")
            } else {
                isInitialized = true
                
                // Set up progress listener to track when speaking starts/stops
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentlySpeakingMessageId.value = null
                    }

                    @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId)"))
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentlySpeakingMessageId.value = null
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        super.onStop(utteranceId, interrupted)
                        _isSpeaking.value = false
                        _currentlySpeakingMessageId.value = null
                    }
                })
            }
        } else {
            Log.e("TTSManager", "Initialization failed")
        }
    }

    fun setSpeed(speed: Float) {
        if (isInitialized) {
            tts?.setSpeechRate(speed)
        }
    }

    fun speak(text: String, messageId: Long) {
        if (!isInitialized) return
        
        // If we are already speaking this message, ignore
        if (_currentlySpeakingMessageId.value == messageId) return
        
        // Stop any current speech
        stop()
        
        // Enqueue the new speech
        _currentlySpeakingMessageId.value = messageId
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId.toString())
    }

    fun stop() {
        if (isInitialized && tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
        _currentlySpeakingMessageId.value = null
    }

    fun shutdown() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
    }
}
