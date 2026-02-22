package com.example.hybridai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hybridai.MainViewModel
import com.example.hybridai.ui.chat.ChatScreen
import com.example.hybridai.ui.settings.SettingsScreen

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CHAT) {

        composable(Routes.CHAT) {
            val messages by mainViewModel.messages.collectAsState()
            val isLoading by mainViewModel.isLoading.collectAsState()

            ChatScreen(
                messages = messages,
                isLoading = isLoading,
                onSendMessage = { prompt -> mainViewModel.sendMessage(prompt) },
                onClearChat = { mainViewModel.clearChat() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
