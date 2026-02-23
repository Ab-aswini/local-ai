package com.example.hybridai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hybridai.core.TaskOrchestrator
import com.example.hybridai.data.db.ChatDatabase
import com.example.hybridai.data.db.ChatMessageEntity
import com.example.hybridai.data.db.ChatRepository
import com.example.hybridai.ui.chat.ChatMessage
import com.example.hybridai.ui.chat.MessageRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.hybridai.core.TextToSpeechManager
import com.example.hybridai.data.AppPreferences

class MainViewModel(
    private val taskOrchestrator: TaskOrchestrator,
    private val repository: ChatRepository,
    private val prefs: AppPreferences,
    private val ttsManager: TextToSpeechManager
) : ViewModel() {

    // ── UI State ─────────────────────────────────────────────────────────

    /** Messages displayed in current chat session */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** Whether AI is actively generating a response */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Speed of last inference run (tokens/sec) */
    private val _tokensPerSecond = MutableStateFlow(0f)
    val tokensPerSecond: StateFlow<Float> = _tokensPerSecond.asStateFlow()

    /** Tracks context window usage when using the local engine */
    private val _contextUsage = MutableStateFlow(0f)
    val contextUsage: StateFlow<Float> = _contextUsage.asStateFlow()

    /** TTS States */
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val currentlySpeakingMessageId: StateFlow<Long?> = ttsManager.currentlySpeakingMessageId

    /** Active session ID — created on first use */
    private var currentSessionId: Long = -1L

    /** Coroutine job for ongoing generation — used to cancel */
    private var generationJob: Job? = null

    // ── Session Management ────────────────────────────────────────────────

    fun startNewSession() {
        viewModelScope.launch {
            currentSessionId = repository.createSession()
            _messages.value = emptyList()
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            currentSessionId = sessionId
            repository.getMessages(sessionId).collect { entities ->
                _messages.value = entities.map { it.toChatMessage() }
            }
        }
    }

    // ── Message Sending ───────────────────────────────────────────────────

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            // Ensure we have an active session
            if (currentSessionId == -1L) {
                currentSessionId = repository.createSession()
            }

            // Save user message to DB
            repository.addMessage(currentSessionId, "user", prompt)

            // Add user bubble to UI
            _messages.update { it + ChatMessage(role = MessageRole.USER, content = prompt) }

            // Add empty AI bubble (will be filled as tokens arrive)
            _messages.update { it + ChatMessage(role = MessageRole.ASSISTANT_LOCAL, content = "") }
            _isLoading.value = true

            val responseStart = System.currentTimeMillis()
            var tokenCount = 0
            var finalResponse = ""

            generationJob = launch {
                try {
                    val maxTokens = prefs.inferenceMaxTokens.first()
                    
                    taskOrchestrator.processInput(prompt).collect { token ->
                        finalResponse += token
                        tokenCount++
                        _messages.update { list ->
                            list.toMutableList().also {
                                it[it.lastIndex] = it.last().copy(content = finalResponse)
                            }
                        }
                        
                        // Force stop early if maxTokens is set and we've reached it
                        if (maxTokens > 0 && tokenCount >= maxTokens) {
                            stopGeneration()
                            return@collect
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Generation was cancelled by user — this is expected, don't rethrow
                } catch (e: Exception) {
                    // Any unexpected inference error — show it in the chat bubble
                    _messages.update { list ->
                        list.toMutableList().apply {
                            if (isNotEmpty()) {
                                this[lastIndex] = last().copy(content = "⚠️ Error: ${e.message}")
                            }
                        }
                    }
                } finally {
                    // Use NonCancellable so suspend calls here succeed even if job was cancelled
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        try {
                            val elapsed = (System.currentTimeMillis() - responseStart) / 1000f
                            val tps = if (elapsed > 0) tokenCount / elapsed else 0f
                            _tokensPerSecond.value = tps

                            // Save performance stats if we're using the local model
                            if (!taskOrchestrator.lastUsedCloud) {
                                try {
                                    val modelName = prefs.selectedModelName.first()
                                    if (modelName.isNotBlank() && tps > 0f) {
                                        prefs.saveModelPerformance(modelName, tps)
                                    }
                                } catch (e: Exception) { /* Ignore */ }
                            }

                            // Safely get the last message
                            val lastMsg = _messages.value.lastOrNull()

                            // Detect which role was used & update bubble color
                            val role = if (taskOrchestrator.lastUsedCloud) "assistant_online" else "assistant_local"
                            if (lastMsg != null && role == "assistant_online") {
                                _messages.update { list ->
                                    list.toMutableList().also {
                                        it[it.lastIndex] = lastMsg.copy(role = MessageRole.ASSISTANT_ONLINE)
                                    }
                                }
                            }

                            // Persist AI response to DB
                            var insertedMessageId = -1L
                            if (finalResponse.isNotBlank()) {
                                insertedMessageId = repository.addMessage(currentSessionId, role, finalResponse, tps)
                                
                                // Update the UI model with the actual DB ID so TTS can track it
                                val finalUiMsg = _messages.value.lastOrNull()
                                if (finalUiMsg != null) {
                                    _messages.update { list ->
                                        list.toMutableList().also {
                                            it[it.lastIndex] = finalUiMsg.copy(id = insertedMessageId)
                                        }
                                    }
                                }
                            }
                            _isLoading.value = false

                            // Auto-speak if enabled
                            if (insertedMessageId != -1L && finalResponse.isNotBlank()) {
                                try {
                                    val ttsEnabled = prefs.ttsEnabled.first()
                                    if (ttsEnabled) {
                                        val speed = prefs.ttsSpeed.first()
                                        ttsManager.setSpeed(speed)
                                        ttsManager.speak(finalResponse, insertedMessageId)
                                    }
                                } catch (e: Exception) { /* TTS failures are non-fatal */ }
                            }
                        } catch (e: Exception) {
                            // Safety net — ensure isLoading is always reset
                            _isLoading.value = false
                        }
                    }
                }
            }
        }
    }

    /** Cancels ongoing generation immediately */
    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isLoading.value = false
    }

    /** Regenerates the last AI response */
    fun regenerateLastResponse() {
        val msgs = _messages.value
        // Find the last user message
        val lastUserMsg = msgs.lastOrNull { it.role == MessageRole.USER } ?: return
        // Remove all messages after it
        _messages.update { list -> list.take(list.indexOfLast { it.role == MessageRole.USER }) }
        sendMessage(lastUserMsg.content)
    }

    fun clearChat() {
        viewModelScope.launch {
            if (currentSessionId != -1L) {
                repository.clearSession(currentSessionId)
            }
            _messages.value = emptyList()
            _tokensPerSecond.value = 0f
            _contextUsage.value = 0f
        }
    }

    /** Deletes a specific message by ID and removes it from the UI */
    fun deleteMessage(msgId: Long) {
        viewModelScope.launch {
            if (currentSessionId != -1L) {
                repository.deleteMessage(msgId)
            }
            if (ttsManager.currentlySpeakingMessageId.value == msgId) {
                ttsManager.stop()
            }
            // To simplify UI updates since our ChatMessage currently doesn't hold the DB ID, 
            // we reload the session to get the latest messages matching the DB state.
            if (currentSessionId != -1L) {
                loadSession(currentSessionId)
            }
        }
    }

    // ── TTS Controls ──────────────────────────────────────────────────────
    
    fun speakMessage(text: String, messageId: Long) {
        viewModelScope.launch {
            val speed = prefs.ttsSpeed.first()
            ttsManager.setSpeed(speed)
            ttsManager.speak(text, messageId)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    // ── Factory ───────────────────────────────────────────────────────────

    class Factory(
        private val context: Context,
        private val taskOrchestrator: TaskOrchestrator,
        private val ttsManager: TextToSpeechManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = ChatDatabase.getInstance(context)
            val repo = ChatRepository(db.sessionDao(), db.messageDao())
            val prefs = AppPreferences(context)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(taskOrchestrator, repo, prefs, ttsManager) as T
        }
    }
}

// Extension: convert DB entity to UI model
private fun ChatMessageEntity.toChatMessage() = ChatMessage(
    id = this.id,
    role = when (this.role) {
        "assistant_online" -> MessageRole.ASSISTANT_ONLINE
        "assistant_local"  -> MessageRole.ASSISTANT_LOCAL
        else               -> MessageRole.USER
    },
    content = this.content,
    timestamp = this.timestamp
)
