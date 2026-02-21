package com.example.hybridai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.hybridai.core.TaskOrchestrator
import com.example.hybridai.local.LocalInferenceManager
import com.example.hybridai.remote.OnlineApiClient
import com.example.hybridai.ui.chat.ChatScreen
import com.example.hybridai.ui.theme.HybridAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var localInferenceManager: LocalInferenceManager
    private lateinit var onlineApiClient: OnlineApiClient
    private lateinit var taskOrchestrator: TaskOrchestrator

    // Custom ViewModel Factory for manual dependency injection
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(taskOrchestrator) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Dependency Infrastructure
        localInferenceManager = LocalInferenceManager(applicationContext)
        onlineApiClient = OnlineApiClient()
        taskOrchestrator = TaskOrchestrator(localInferenceManager, onlineApiClient)

        // 2. Start Hardware-dependent initialization
        lifecycleScope.launch {
            localInferenceManager.initialize()
        }

        // 3. Set UI Content
        setContent {
            HybridAITheme {
                val messages by viewModel.messages.collectAsState()
                
                ChatScreen(
                    messages = messages,
                    onSendMessage = { prompt ->
                        viewModel.sendMessage(prompt)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localInferenceManager.shutdown()
    }
}
