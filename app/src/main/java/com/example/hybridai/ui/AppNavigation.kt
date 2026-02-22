package com.example.hybridai.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hybridai.MainViewModel
import com.example.hybridai.data.AppPreferences
import com.example.hybridai.ui.chat.ChatScreen
import com.example.hybridai.ui.onboarding.OnboardingScreen
import com.example.hybridai.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val CHAT       = "chat"
    const val SETTINGS   = "settings"
}

@Composable
fun AppNavigation(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val context       = LocalContext.current
    val prefs         = remember { AppPreferences(context) }
    val scope         = rememberCoroutineScope()

    // Read first-launch flag once on start
    val hasSeenOnboarding by prefs.hasSeenOnboarding.collectAsState(initial = null)

    // Wait until preference is loaded before deciding the start destination
    val startDestination = when (hasSeenOnboarding) {
        null  -> return   // Still loading — render nothing until resolved
        false -> Routes.ONBOARDING
        else  -> Routes.CHAT
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    scope.launch { prefs.markOnboardingSeen() }
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHAT) {
            val messages        by mainViewModel.messages.collectAsState()
            val isLoading       by mainViewModel.isLoading.collectAsState()
            val tokensPerSecond by mainViewModel.tokensPerSecond.collectAsState()

            ChatScreen(
                messages             = messages,
                isLoading            = isLoading,
                tokensPerSecond      = tokensPerSecond,
                onSendMessage        = { prompt -> mainViewModel.sendMessage(prompt) },
                onClearChat          = { mainViewModel.clearChat() },
                onOpenSettings       = { navController.navigate(Routes.SETTINGS) },
                onStopGeneration     = { mainViewModel.stopGeneration() },
                onRegenerateResponse = { mainViewModel.regenerateLastResponse() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
