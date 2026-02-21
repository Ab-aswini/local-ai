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

    fun sendMessage(prompt: String) {
        // 1. Add User Message
        val updatedList = _messages.value.toMutableList()
        updatedList.add(ChatMessage(MessageRole.USER, prompt))
        _messages.value = updatedList

        // 2. Add Empty Assistant Message that we will stream into
        // We do a simple proxy check here for the UI indicator (Local vs Online).
        // Ideally, the Orchestrator returns a Pair<EngineType, Flow<String>>.
        val role = if (prompt.length > 500 || prompt.lowercase().contains("code") || prompt.lowercase().contains("analyze")) {
            MessageRole.ASSISTANT_ONLINE
        } else {
            MessageRole.ASSISTANT_LOCAL
        }
        
        updatedList.add(ChatMessage(role, ""))
        _messages.value = updatedList
        
        val newMsgIndex = updatedList.lastIndex

        // 3. Collect Stream
        viewModelScope.launch {
            taskOrchestrator.processPrompt(prompt).collect { token ->
                val currentList = _messages.value.toMutableList()
                val currentMessage = currentList[newMsgIndex]
                currentList[newMsgIndex] = currentMessage.copy(content = currentMessage.content + token)
                _messages.value = currentList
            }
        }
    }
}
