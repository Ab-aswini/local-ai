package com.example.hybridai.data.db

import kotlinx.coroutines.flow.Flow

/**
 * Repository layer between ViewModel and Room DAOs.
 * Handles session lifecycle (create, title auto-update, delete).
 */
class ChatRepository(
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao
) {
    // ── Sessions ──────────────────────────────────────────────────────────

    val allSessions: Flow<List<ChatSessionEntity>> = sessionDao.getAllSessions()

    suspend fun createSession(): Long {
        return sessionDao.insertSession(ChatSessionEntity())
    }

    suspend fun deleteSession(sessionId: Long) {
        messageDao.deleteMessagesForSession(sessionId)
        sessionDao.deleteSession(sessionId)
    }

    // ── Messages ──────────────────────────────────────────────────────────

    fun getMessages(sessionId: Long): Flow<List<ChatMessageEntity>> =
        messageDao.getMessagesForSession(sessionId)

    suspend fun addMessage(
        sessionId: Long,
        role: String,
        content: String,
        tokensPerSecond: Float = 0f
    ): Long {
        val msg = ChatMessageEntity(
            sessionId = sessionId,
            role = role,
            content = content,
            tokensPerSecond = tokensPerSecond
        )
        val id = messageDao.insertMessage(msg)

        // Auto-title session from first user message
        if (role == "user" && messageDao.countMessages(sessionId) == 1) {
            val title = content.take(40).trimEnd().let {
                if (content.length > 40) "$it…" else it
            }
            sessionDao.updateTitle(sessionId, title)
        } else {
            sessionDao.touch(sessionId)
        }

        return id
    }

    suspend fun clearSession(sessionId: Long) {
        messageDao.deleteMessagesForSession(sessionId)
        sessionDao.updateTitle(sessionId, "New Chat")
    }
}
