package com.example.hybridai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun sessionDao(): ChatSessionDao
    abstract fun messageDao(): ChatMessageDao

    companion object {
        @Volatile private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "hybrid_ai_chat.db"
                )
                    .fallbackToDestructiveMigration() // simple for now; use migrations in v1.0
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
