package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.QRMasterViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PasscodeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Full bleed Edge-to-Edge experience
        enableEdgeToEdge()

        setContent {
            val viewModel: QRMasterViewModel = viewModel()
            val isDarkTheme by viewModel.themeDark.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialThemeColor()
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        // 1. SPLASH SCREEN INTRODUCTIONS
                        composable("splash") {
                            SplashScreen(
                                viewModel = viewModel,
                                onNavigateToOnboarding = {
                                    navController.navigate("onboarding") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToPasscode = {
                                    navController.navigate("passcode") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToMain = {
                                    navController.navigate("main") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. ONBOARDING TUTORIALS
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onNavigateToMain = {
                                    navController.navigate("main") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 3. SECURE PASSCODE APP LOCK
                        composable("passcode") {
                            PasscodeScreen(
                                viewModel = viewModel,
                                onUnlockSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("passcode") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 4. MAIN APP HOME WORKSPACE
                        composable("main") {
                            MainScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialThemeColor() = androidx.compose.material3.MaterialTheme.colorScheme.background
