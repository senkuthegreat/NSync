package com.example.nsutconnect

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.sync.NsutSyncRepository
import com.example.nsutconnect.data.sync.SyncResult
import com.example.nsutconnect.theme.NSUTConnectTheme
import com.example.nsutconnect.ui.auth.LoginScreen
import com.example.nsutconnect.ui.main.MainScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var activeTab by mutableStateOf("attendance")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        activeTab = intent?.getStringExtra("destination_tab") ?: "attendance"

        setContent {
            NSUTConnectTheme {
                val coroutineScope = rememberCoroutineScope()
                val prefs = remember { UserPreferences(this@MainActivity) }
                val repository = remember { NsutSyncRepository(this@MainActivity) }
                var isLoggedIn by remember { mutableStateOf(prefs.isLoggedIn()) }
                var isLoggingIn by remember { mutableStateOf(false) }
                var loginErrorMessage by remember { mutableStateOf<String?>(null) }

                var captchaSession by remember { mutableStateOf<com.example.nsutconnect.data.network.CaptchaSession?>(null) }
                var isCaptchaLoading by remember { mutableStateOf(false) }

                fun refreshCaptcha() {
                    coroutineScope.launch {
                        isCaptchaLoading = true
                        captchaSession = repository.getCaptchaSession()
                        isCaptchaLoading = false
                    }
                }

                LaunchedEffect(isLoggedIn) {
                    if (!isLoggedIn) {
                        refreshCaptcha()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn) {
                        MainScreen(
                            initialTab = activeTab,
                            onLogout = {
                                repository.logout()
                                isLoggedIn = false
                                refreshCaptcha()
                            }
                        )
                    } else {
                        LoginScreen(
                            initialRollNumber = prefs.getRollNumber(),
                            initialPassword = "",
                            isLoading = isLoggingIn,
                            errorMessage = loginErrorMessage,
                            captchaSession = captchaSession,
                            isCaptchaLoading = isCaptchaLoading,
                            onRefreshCaptcha = { refreshCaptcha() },
                            onLogin = { roll, pass, cap, auto ->
                                coroutineScope.launch {
                                    isLoggingIn = true
                                    loginErrorMessage = null
                                    val result = repository.login(roll, pass, cap, captchaSession, auto)
                                    isLoggingIn = false
                                    when (result) {
                                        is SyncResult.Success -> {
                                            isLoggedIn = true
                                        }
                                        is SyncResult.Error -> {
                                            loginErrorMessage = result.error
                                            refreshCaptcha()
                                        }
                                        SyncResult.Loading -> {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("destination_tab")?.let {
            activeTab = it
        }
    }
}
