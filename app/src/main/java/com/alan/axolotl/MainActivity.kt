package com.alan.axolotl

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alan.axolotl.service.TimerService
import com.alan.axolotl.ui.theme.AxolotlTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val _showLock = MutableStateFlow(false)
    val showLock = _showLock.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleTimerIntent(intent)
        setContent {
            AxolotlTheme {
                AxolotlApp(
                    showLockFlow = showLock,
                    onLockConsumed = { _showLock.value = false },
                    onLockEngaged = { startScreenPinning() },
                    onLockDisengaged = { stopScreenPinning() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleTimerIntent(intent)
    }

    private fun handleTimerIntent(intent: Intent?) {
        if (intent?.action == TimerService.ACTION_TIMER_FINISHED) {
            _showLock.value = true
        }
    }

    private fun startScreenPinning() {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
            startLockTask()
        }
    }

    private fun stopScreenPinning() {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            stopLockTask()
        }
    }
}
