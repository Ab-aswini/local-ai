package com.example.hybridai.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hybridai.ui.theme.*

private data class OnboardSlide(
    val emoji: String,
    val title: String,
    val subtitle: String
)

private val slides = listOf(
    OnboardSlide(
        emoji     = "🤖",
        title     = "Meet Your Private AI",
        subtitle  = "Local AI runs entirely on your device.\nNo internet needed. No data leaves your phone."
    ),
    OnboardSlide(
        emoji     = "⚡",
        title     = "Hybrid Intelligence",
        subtitle  = "Simple questions? Answered locally in seconds.\nComplex tasks? Routed to Gemini cloud automatically."
    ),
    OnboardSlide(
        emoji     = "🚀",
        title     = "You're Ready",
        subtitle  = "Download a model from Settings → Local Models.\nOr paste your Gemini API key to start immediately."
    )
)

/**
 * 3-slide onboarding shown on first app launch.
 * Calls [onFinish] when the user taps "Get Started" on the last slide.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val slide = slides[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(32.dp))

        // ── Slide content (animated) ──────────────────────────────────────
        AnimatedContent(
            targetState = slide,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
            label = "slide"
        ) { s ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(s.emoji, fontSize = 80.sp)
                Text(
                    text       = s.title,
                    color      = PrimaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 26.sp,
                    textAlign  = TextAlign.Center
                )
                Text(
                    text      = s.subtitle,
                    color     = SecondaryAccent,
                    fontSize  = 15.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Dot indicators ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slides.indices.forEach { i ->
                    val color by animateColorAsState(
                        targetValue = if (i == page) LocalIndicator else SurfaceGray,
                        animationSpec = tween(300),
                        label = "dot_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // ── Navigation button ─────────────────────────────────────────
            Button(
                onClick = {
                    if (page < slides.lastIndex) page++ else onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalIndicator,
                    contentColor   = TrueBlack
                )
            ) {
                Text(
                    text       = if (page < slides.lastIndex) "Next →" else "Get Started",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }

            if (page < slides.lastIndex) {
                TextButton(onClick = onFinish) {
                    Text("Skip", color = SecondaryAccent, fontSize = 13.sp)
                }
            }
        }
    }
}
