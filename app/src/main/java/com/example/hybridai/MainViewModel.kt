package com.example.hybridai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hybridai.core.TaskOrchestrator
import com.example.hybridai.ui.chat.ChatMessage
import com.example.hybridai.ui.chat.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val taskOrchestrator: TaskOrchestrator
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // True while the AI is generating a response
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(prompt: String) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) return

        // 1. Add user message to history
        val updatedList = _messages.value.toMutableList()
        updatedList.add(ChatMessage(MessageRole.USER, trimmedPrompt))
        _messages.value = updatedList

        // 2. Determine routing for the indicator dot (Local 🟢 vs Online 🔵)
        val isComplex = trimmedPrompt.length > 100
                || trimmedPrompt.lowercase().let { p ->
            listOf("code", "analyze", "explain", "write", "how do", "what is", "compare").any { p.contains(it) }
        }
        val role = if (isComplex) MessageRole.ASSISTANT_ONLINE else MessageRole.ASSISTANT_LOCAL

        viewModelScope.launch {
            _isLoading.value = true

            // 3. Add empty assistant message — we stream tokens into it
            val streamingList = _messages.value.toMutableList()
            streamingList.add(ChatMessage(role, ""))
            _messages.value = streamingList
            val assistantIndex = streamingList.lastIndex

            // 4. Collect streamed tokens and append them to the message
            try {
                taskOrchestrator.processPrompt(trimmedPrompt).collect { token ->
                    val currentList = _messages.value.toMutableList()
                    val current = currentList[assistantIndex]
                    currentList[assistantIndex] = current.copy(content = current.content + token)
                    _messages.value = currentList
                }
            } catch (e: Exception) {
                val currentList = _messages.value.toMutableList()
                currentList[assistantIndex] = ChatMessage(role, "Error: ${e.message}")
                _messages.value = currentList
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
