package com.example.hybridai.ui.chat

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.hybridai.ui.theme.LocalIndicator
import com.example.hybridai.ui.theme.SecondaryAccent
import com.example.hybridai.ui.theme.TrueBlack
import java.util.Locale

/**
 * Mic button that triggers Android's built-in speech recognition.
 * No extra permissions needed — RecognizerIntent handles it.
 * Calls [onResult] with the transcribed text when recognition succeeds.
 */
@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    // Animate mic while listening
    val scale by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!text.isNullOrBlank()) onResult(text)
    }

    val isSpeechAvailable = remember {
        SpeechRecognizer.isRecognitionAvailable(context)
    }

    if (!isSpeechAvailable) {
        // Greyed out if device doesn't support speech
        Icon(
            Icons.Default.MicOff,
            contentDescription = "Speech not available",
            tint = SecondaryAccent.copy(alpha = 0.4f),
            modifier = modifier.size(24.dp)
        )
        return
    }

    IconButton(
        onClick = {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechLauncher.launch(intent)
        },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Voice input",
            tint = if (isListening) LocalIndicator else SecondaryAccent,
            modifier = Modifier
                .size(22.dp)
                .then(if (isListening) Modifier.scale(scale) else Modifier)
        )
    }
}
