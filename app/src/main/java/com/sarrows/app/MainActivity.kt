package com.sarrows.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.sarrows.app.ui.navigation.AppNavigation
import com.sarrows.app.ui.navigation.Routes
import com.sarrows.app.ui.theme.SarrowsTheme
import com.sarrows.app.ui.viewmodels.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SarrowsTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val uiState by authViewModel.uiState.collectAsState()

                // Keep splash screen visible while checking session
                var isReady by remember { mutableStateOf(false) }
                splashScreen.setKeepOnScreenCondition { !isReady }

                LaunchedEffect(uiState) {
                    if (uiState !is AuthUiState.Loading) {
                        isReady = true
                    }
                }

                if (isReady) {
                    val startDestination = when (uiState) {
                        is AuthUiState.Success -> Routes.HOME
                        else -> Routes.HOME // Allow browsing without login; auth gated on stream
                    }
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}
