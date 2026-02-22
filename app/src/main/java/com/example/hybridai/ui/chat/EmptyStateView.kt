package com.example.hybridai.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hybridai.ui.theme.*

/**
 * Shown in the middle of the chat when there are no messages.
 * Animates in with a gentle fade + pulse on the emoji.
 */
@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val quickSuggestions = listOf(
        "🤔  Ask me anything",
        "💻  Generate code",
        "✍️   Write something creative",
        "🔍  Explain a concept"
    )

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🤖",
            fontSize = 64.sp,
            modifier = Modifier.alpha(pulse)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Local AI",
            color = PrimaryAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Your private AI assistant.\nRuns locally — no data ever leaves your phone.",
            color = SecondaryAccent,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(28.dp))

        quickSuggestions.forEach { hint ->
            Text(
                text = hint,
                color = SecondaryAccent.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
    }
}
