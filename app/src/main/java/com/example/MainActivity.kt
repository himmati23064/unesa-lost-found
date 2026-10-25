package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.model.Report
import com.example.ui.screens.AccountManagementScreen
import com.example.ui.screens.AddReportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ReportViewModel

enum class AppScreen {
    LOGIN,
    HOME,
    ADD_REPORT,
    ACCOUNT_MANAGEMENT
}

class MainActivity : ComponentActivity() {

    private val viewModel: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                    var currentScreen by remember(isLoggedIn) {
                        mutableStateOf(if (isLoggedIn) AppScreen.HOME else AppScreen.LOGIN)
                    }
                    var initialReportType by remember { mutableStateOf(Report.TYPE_LOST) }

                    when (currentScreen) {
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        }

                        AppScreen.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAddReport = { type ->
                                    initialReportType = type
                                    currentScreen = AppScreen.ADD_REPORT
                                },
                                onNavigateToAccount = {
                                    currentScreen = AppScreen.ACCOUNT_MANAGEMENT
                                }
                            )
                        }

                        AppScreen.ADD_REPORT -> {
                            BackHandler {
                                currentScreen = AppScreen.HOME
                            }
                            AddReportScreen(
                                viewModel = viewModel,
                                initialType = initialReportType,
                                onNavigateBack = {
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        }

                        AppScreen.ACCOUNT_MANAGEMENT -> {
                            BackHandler {
                                currentScreen = AppScreen.HOME
                            }
                            AccountManagementScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    currentScreen = AppScreen.HOME
                                },
                                onLogoutSuccess = {
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
