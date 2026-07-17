package com.alan.axolotl.ui.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import com.alan.axolotl.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimerUiState(
    val selectedMinutes: Int = 5,
    val remainingSeconds: Long = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class TimerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var receiver: BroadcastReceiver? = null
    private var registeredContext: Context? = null

    fun setMinutes(minutes: Int) {
        if (!_uiState.value.isRunning) {
            _uiState.update { it.copy(selectedMinutes = minutes.coerceIn(1, 60)) }
        }
    }

    fun incrementMinutes() {
        setMinutes(_uiState.value.selectedMinutes + 1)
    }

    fun decrementMinutes() {
        setMinutes(_uiState.value.selectedMinutes - 1)
    }

    fun startTimer(context: Context) {
        val minutes = _uiState.value.selectedMinutes
        _uiState.update {
            it.copy(
                isRunning = true,
                isFinished = false,
                remainingSeconds = minutes * 60L
            )
        }
        registerReceiver(context)
        TimerService.startTimer(context, minutes)
    }

    fun cancelTimer(context: Context) {
        TimerService.stopTimer(context)
        _uiState.update {
            it.copy(isRunning = false, isFinished = false, remainingSeconds = 0)
        }
    }

    fun registerReceiver(context: Context) {
        if (receiver != null) return
        val appContext = context.applicationContext
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == TimerService.ACTION_TICK) {
                    val remaining = intent.getLongExtra(TimerService.EXTRA_REMAINING, 0)
                    val running = intent.getBooleanExtra(TimerService.EXTRA_RUNNING, false)
                    _uiState.update {
                        it.copy(
                            remainingSeconds = remaining,
                            isRunning = running,
                            isFinished = !running && remaining == 0L && it.isRunning
                        )
                    }
                }
            }
        }
        appContext.registerReceiver(
            receiver,
            IntentFilter(TimerService.ACTION_TICK),
            Context.RECEIVER_NOT_EXPORTED
        )
        registeredContext = appContext
    }

    fun resetAfterLock() {
        _uiState.update { it.copy(isFinished = false) }
    }

    override fun onCleared() {
        super.onCleared()
        receiver?.let { registeredContext?.unregisterReceiver(it) }
        receiver = null
        registeredContext = null
    }
}
