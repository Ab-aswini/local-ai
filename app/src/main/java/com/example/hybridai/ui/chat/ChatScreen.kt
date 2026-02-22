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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hybridai.ui.theme.*
import kotlinx.coroutines.launch

// Data model for messages
enum class MessageRole { USER, ASSISTANT_LOCAL, ASSISTANT_ONLINE }
data class ChatMessage(val role: MessageRole, val content: String)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // Top App Bar
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
                Text(
                    text = if (isLoading) "Thinking..." else "🟢 Local  🔵 Cloud",
                    color = SecondaryAccent,
                    fontSize = 12.sp
                )
            }
            Row {
                IconButton(onClick = onClearChat) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear chat", tint = SecondaryAccent, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
            // Show typing animation at the bottom while loading
            if (isLoading && messages.lastOrNull()?.content?.isEmpty() == true) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Ask something...", color = SecondaryAccent, fontSize = 15.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isLoading) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceGray,
                    unfocusedContainerColor = SurfaceGray,
                    focusedTextColor = PrimaryAccent,
                    unfocusedTextColor = PrimaryAccent,
                    cursorColor = PrimaryAccent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isLoading) SurfaceGray else PrimaryAccent),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (isLoading) SecondaryAccent else TrueBlack
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val backgroundColor = if (isUser) UserMessageColor else SurfaceGray

    val indicatorColor = when (message.role) {
        MessageRole.ASSISTANT_LOCAL -> LocalIndicator
        MessageRole.ASSISTANT_ONLINE -> OnlineIndicator
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .align(Alignment.Top)
                    .padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(backgroundColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                color = PrimaryAccent,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f, label = "d1",
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f, label = "d2",
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse)
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f, label = "d3",
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LocalIndicator.copy(alpha = dot1Alpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LocalIndicator.copy(alpha = dot2Alpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LocalIndicator.copy(alpha = dot3Alpha))
        )
    }
}
