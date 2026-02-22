package com.example.hybridai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.hybridai.core.TaskOrchestrator
import com.example.hybridai.data.AppPreferences
import com.example.hybridai.local.LocalInferenceManager
import com.example.hybridai.remote.OnlineApiClient
import com.example.hybridai.ui.AppNavigation
import com.example.hybridai.ui.theme.HybridAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appPreferences: AppPreferences
    private lateinit var localInferenceManager: LocalInferenceManager
    private lateinit var onlineApiClient: OnlineApiClient
    private lateinit var taskOrchestrator: TaskOrchestrator

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

        // Initialize preferences and dependencies
        appPreferences = AppPreferences(applicationContext)
        onlineApiClient = OnlineApiClient(appPreferences)
        localInferenceManager = LocalInferenceManager(applicationContext)
        taskOrchestrator = TaskOrchestrator(localInferenceManager, onlineApiClient)

        lifecycleScope.launch {
            localInferenceManager.initialize()
        }

        setContent {
            HybridAITheme {
                AppNavigation(mainViewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localInferenceManager.shutdown()
    }
}
