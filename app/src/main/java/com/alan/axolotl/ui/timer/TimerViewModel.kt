package com.alan.axolotl.ui.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import com.alan.axolotl.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TimerUiState(
    val selectedMinutes: Int = 5,
    val remainingSeconds: Long = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var receiver: BroadcastReceiver? = null

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

    fun startTimer() {
        val minutes = _uiState.value.selectedMinutes
        _uiState.update {
            it.copy(
                isRunning = true,
                isFinished = false,
                remainingSeconds = minutes * 60L
            )
        }
        registerReceiver()
        TimerService.startTimer(context, minutes)
    }

    fun cancelTimer() {
        TimerService.stopTimer(context)
        _uiState.update {
            it.copy(isRunning = false, isFinished = false, remainingSeconds = 0)
        }
    }

    fun registerReceiver() {
        if (receiver != null) return
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
        context.registerReceiver(
            receiver,
            IntentFilter(TimerService.ACTION_TICK),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun resetAfterLock() {
        _uiState.update { it.copy(isFinished = false) }
    }

    override fun onCleared() {
        super.onCleared()
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}
