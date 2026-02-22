package com.example.hybridai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single chat session — holds the session ID and title.
 * Title is auto-generated from the first user message.
 */
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A single message in a chat session.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,              // FK → ChatSessionEntity.id
    val role: String,                 // "user" | "assistant_local" | "assistant_online"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensPerSecond: Float = 0f   // stored for display
)
