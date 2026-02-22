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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val taskOrchestrator: TaskOrchestrator,
    private val repository: ChatRepository
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
            _messages.update { it + ChatMessage(MessageRole.USER, prompt) }

            // Add empty AI bubble (will be filled as tokens arrive)
            _messages.update { it + ChatMessage(MessageRole.ASSISTANT_LOCAL, "") }
            _isLoading.value = true

            val responseStart = System.currentTimeMillis()
            var tokenCount = 0
            var finalResponse = ""

            generationJob = launch {
                try {
                    taskOrchestrator.processInput(prompt).collect { token ->
                        finalResponse += token
                        tokenCount++
                        _messages.update { list ->
                            list.toMutableList().also {
                                it[it.lastIndex] = it.last().copy(content = finalResponse)
                            }
                        }
                    }
                } finally {
                    val elapsed = (System.currentTimeMillis() - responseStart) / 1000f
                    val tps = if (elapsed > 0) tokenCount / elapsed else 0f
                    _tokensPerSecond.value = tps

                    // Detect which role was used (update last message role from orchestrator)
                    val role = if (taskOrchestrator.lastUsedCloud) "assistant_online" else "assistant_local"
                    val finalMsg = _messages.value.last()
                    if (role == "assistant_online") {
                        _messages.update { list ->
                            list.toMutableList().also {
                                it[it.lastIndex] = finalMsg.copy(
                                    role = MessageRole.ASSISTANT_ONLINE
                                )
                            }
                        }
                    }

                    // Persist AI response to DB
                    if (finalResponse.isNotBlank()) {
                        repository.addMessage(currentSessionId, role, finalResponse, tps)
                    }
                    _isLoading.value = false
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
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────

    class Factory(
        private val context: Context,
        private val taskOrchestrator: TaskOrchestrator
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = ChatDatabase.getInstance(context)
            val repo = ChatRepository(db.sessionDao(), db.messageDao())
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(taskOrchestrator, repo) as T
        }
    }
}

// Extension: convert DB entity to UI model
private fun ChatMessageEntity.toChatMessage() = ChatMessage(
    role = when (role) {
        "user" -> MessageRole.USER
        "assistant_online" -> MessageRole.ASSISTANT_ONLINE
        else -> MessageRole.ASSISTANT_LOCAL
    },
    content = content
)
