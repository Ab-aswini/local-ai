package com.example.hybridai.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :time WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE chat_sessions SET updatedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSession(id: Long): ChatSessionEntity?
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun countMessages(sessionId: Long): Int

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstMessage(sessionId: Long): ChatMessageEntity?
}
