package com.example.hybridai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hybridai.core.TaskOrchestrator
import com.example.hybridai.core.TextToSpeechManager
import com.example.hybridai.data.AppPreferences
import com.example.hybridai.local.LocalInferenceManager
import com.example.hybridai.remote.OnlineApiClient
import com.example.hybridai.ui.AppNavigation
import com.example.hybridai.ui.theme.HybridAITheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appPreferences: AppPreferences
    private lateinit var localInferenceManager: LocalInferenceManager
    private lateinit var onlineApiClient: OnlineApiClient
    private lateinit var taskOrchestrator: TaskOrchestrator
    private lateinit var ttsManager: TextToSpeechManager

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(applicationContext, taskOrchestrator, ttsManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appPreferences = AppPreferences(applicationContext)
        onlineApiClient = OnlineApiClient(appPreferences)
        localInferenceManager = LocalInferenceManager(applicationContext)
        taskOrchestrator = TaskOrchestrator(localInferenceManager, onlineApiClient)
        ttsManager = TextToSpeechManager(applicationContext)

        // Combine path, temp, and context size to trigger a unified reload
        lifecycleScope.launch {
            combine(
                appPreferences.selectedModelPath,
                appPreferences.inferenceTemperature,
                appPreferences.inferenceContextSize
            ) { path, temp, ctx ->
                Triple(path, temp, ctx)
            }
            .distinctUntilChanged()
            .collect {
                localInferenceManager.reloadModel()
            }
        }

        // Start a new chat session when app opens
        viewModel.startNewSession()

        setContent {
            HybridAITheme {
                AppNavigation(mainViewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localInferenceManager.shutdown()
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }
    }
}
