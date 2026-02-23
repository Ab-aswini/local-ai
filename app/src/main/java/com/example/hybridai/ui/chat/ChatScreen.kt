package com.example.hybridai.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hybridai.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// ── Data models ────────────────────────────────────────────────────────────

enum class MessageRole { USER, ASSISTANT_LOCAL, ASSISTANT_ONLINE }
data class ChatMessage(
    val id: Long = 0L,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ── Main screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    tokensPerSecond: Float,
    contextUsage: Float,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onStopGeneration: () -> Unit = {},
    onRegenerateResponse: () -> Unit = {},
    isSpeaking: Boolean = false,
    speakingMessageId: Long? = null,
    onSpeakMessage: (String, Long) -> Unit = { _, _ -> },
    onStopSpeaking: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copiedMessageIndex by remember { mutableStateOf(-1) }

    // Show scroll-to-bottom FAB when not near the bottom
    val showScrollFab by remember {
        derivedStateOf {
            val lastVisibleIdx = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleIdx < totalItems - 3
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // ── Top App Bar ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Local AI",
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLoading) "Thinking…" else "🟢 Local  🔵 Cloud",
                        color = SecondaryAccent,
                        fontSize = 11.sp
                    )
                    if (tokensPerSecond > 0f && !isLoading) {
                        Text("⚡ ${"%.1f".format(tokensPerSecond)} tok/s",
                            color = LocalIndicator, fontSize = 11.sp)
                    }
                }
                if (contextUsage > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = contextUsage,
                        modifier = Modifier.width(100.dp).height(2.dp).clip(CircleShape),
                        color = LocalIndicator,
                        trackColor = SurfaceGray
                    )
                }
            }
            Row {
                IconButton(onClick = onClearChat) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear chat",
                        tint = SecondaryAccent, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings",
                        tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Message List + FAB overlay ────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            // Empty state shown while no messages exist
            if (messages.isEmpty() && !isLoading) {
                EmptyStateView(
                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages.size, key = { messages[it].id }) { index ->
                    val message = messages[index]
                    val isCopied = copiedMessageIndex == index

                    val dismissState = rememberDismissState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == DismissValue.DismissedToStart) {
                                onDeleteMessage(message.id)
                                true
                            } else false
                        }
                    )

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            val color = if (dismissState.dismissDirection == DismissDirection.EndToStart) 
                                MaterialTheme.colorScheme.errorContainer else Color.Transparent
                            Box(
                                Modifier.fillMaxSize().background(color, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                        dismissContent = {
                        ChatBubble(
                            message = message,
                            isCopied = isCopied,
                            isCurrentlySpeaking = isSpeaking && speakingMessageId == message.id,
                            onCopy = {
                                clipboard.setText(AnnotatedString(message.content))
                                copiedMessageIndex = index
                            },
                            onRegenerate = if (index == messages.lastIndex && message.role != MessageRole.USER) {
                                { onRegenerateResponse() }
                            } else null,
                            onSpeak = {
                                onSpeakMessage(message.content, message.id)
                            },
                            onStopSpeaking = onStopSpeaking
                        )
                    }
                    )
                }

                if (isLoading && (messages.isEmpty() || messages.last().role == MessageRole.USER)) {
                    item { TypingIndicator() }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Scroll-to-bottom FAB
            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollFab,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit  = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch { listState.animateScrollToItem(messages.lastIndex.coerceAtLeast(0)) }
                    },
                    containerColor = SurfaceGray,
                    contentColor = PrimaryAccent
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                }
            }
        }

        // ── Quick Action Chips (shown when input is empty) ────────────────
        if (inputText.isEmpty() && !isLoading) {
            val quickActions = listOf("Summarize this", "Explain step by step",
                "Translate to English", "Fix my code", "Tell me a joke")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TrueBlack)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { action ->
                    FilterChip(
                        selected = false,
                        onClick = { inputText = action },
                        label = { Text(action, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceGray,
                            labelColor = SecondaryAccent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor       = SurfaceGray,
                            selectedBorderColor = SurfaceGray,
                            borderWidth       = 0.dp,
                            selectedBorderWidth = 0.dp
                        )
                    )
                }
            }
        }

        // ── Input Row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask something…", color = SecondaryAccent, fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (!isLoading && inputText.isNotBlank()) {
                        onSendMessage(inputText.trim())
                        inputText = ""
                    }
                }),
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = SurfaceGray,
                    focusedTextColor = PrimaryAccent,
                    unfocusedTextColor = PrimaryAccent,
                    cursorColor = PrimaryAccent,
                    focusedContainerColor = SurfaceGray,
                    unfocusedContainerColor = SurfaceGray
                )
            )

            // 🎙️ Voice input — fills text field; user can edit before sending
            VoiceInputButton(onResult = { inputText = it })

            // Stop button while generating / Send button otherwise
            if (isLoading) {
                FilledIconButton(
                    onClick = onStopGeneration,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
            } else {
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PrimaryAccent,
                        disabledContainerColor = SurfaceGray
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = TrueBlack)
                }
            }
        }
    }
}

// ── Chat Bubble ────────────────────────────────────────────────────────────

@Composable
fun ChatBubble(
    message: ChatMessage,
    isCopied: Boolean,
    isCurrentlySpeaking: Boolean,
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)?,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = when (message.role) {
        MessageRole.USER               -> SurfaceGray
        MessageRole.ASSISTANT_LOCAL    -> DarkGray
        MessageRole.ASSISTANT_ONLINE   -> Color(0xFF0A1628)
    }
    val indicatorColor = when (message.role) {
        MessageRole.ASSISTANT_LOCAL  -> LocalIndicator
        MessageRole.ASSISTANT_ONLINE -> OnlineIndicator
        else                         -> Color.Transparent
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Role dot for AI messages
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp, bottom = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }

            // Message text — use MarkdownText for AI, plain Text for user
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (isUser) {
                    Text(text = message.content, color = PrimaryAccent, fontSize = 14.sp, lineHeight = 20.sp)
                } else {
                    MarkdownText(text = message.content)
                }
            }
        }

        // Timestamp below bubble
        val timeStr = remember(message.timestamp) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        }
        Text(
            text = timeStr,
            fontSize = 10.sp,
            color = SecondaryAccent.copy(alpha = 0.6f),
            modifier = Modifier.padding(
                start = if (isUser) 0.dp else 14.dp,
                end  = if (isUser) 14.dp else 0.dp,
                top = 2.dp
            )
        )

        // Action buttons below AI messages (copy / regenerate)
        if (!isUser && message.content.isNotBlank()) {
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Copy button
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (isCopied) LocalIndicator else SecondaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isCopied) "Copied!" else "Copy",
                        fontSize = 11.sp,
                        color = if (isCopied) LocalIndicator else SecondaryAccent
                    )
                }

                // Regenerate button (only for last AI message)
                if (onRegenerate != null) {
                    TextButton(
                        onClick = onRegenerate,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate",
                            tint = SecondaryAccent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Regenerate", fontSize = 11.sp, color = SecondaryAccent)
                    }
                }

                // Play / Stop speaking button
                TextButton(
                    onClick = if (isCurrentlySpeaking) onStopSpeaking else onSpeak,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        if (isCurrentlySpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentlySpeaking) "Stop speaking" else "Read aloud",
                        tint = if (isCurrentlySpeaking) Color.Red else SecondaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isCurrentlySpeaking) "Stop" else "Listen",
                        fontSize = 11.sp,
                        color = if (isCurrentlySpeaking) Color.Red else SecondaryAccent
                    )
                }
            }
        }
    }
}

// ── Typing Indicator ───────────────────────────────────────────────────────

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlphas = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0.3f at index * 200
                    1f at index * 200 + 300
                    0.3f at 900
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot$index"
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DarkGray)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotAlphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SecondaryAccent.copy(alpha = alpha.value))
            )
        }
    }
}
