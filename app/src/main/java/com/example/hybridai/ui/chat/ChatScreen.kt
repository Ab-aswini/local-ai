package com.example.hybridai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hybridai.ui.theme.*

// Data model for messages
enum class MessageRole { USER, ASSISTANT_LOCAL, ASSISTANT_ONLINE }
data class ChatMessage(val role: MessageRole, val content: String)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(16.dp)
    ) {
        // Chat History
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        // Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Ask something...", color = SecondaryAccent) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkGray,
                    unfocusedContainerColor = DarkGray,
                    focusedTextColor = PrimaryAccent,
                    unfocusedTextColor = PrimaryAccent,
                    cursorColor = PrimaryAccent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("→", color = TrueBlack, fontWeight = FontWeight.Bold)
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
            // Processing Indicator Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    topStart = 16.dp, 
                    topEnd = 16.dp, 
                    bottomStart = if (isUser) 16.dp else 4.dp, 
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ))
                .background(backgroundColor)
                .padding(16.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                color = PrimaryAccent,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
