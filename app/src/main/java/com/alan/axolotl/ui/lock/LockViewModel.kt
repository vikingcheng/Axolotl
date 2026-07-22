package com.alan.axolotl.ui.lock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.random.Random

data class LockUiState(
    val num1: Int = 0,
    val num2: Int = 0,
    val userInput: String = "",
    val isWrongAnswer: Boolean = false
)

@HiltViewModel
class LockViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
        generateNewProblem()
    }

    private fun generateNewProblem() {
        val n1 = Random.nextInt(2, 10)
        val n2 = Random.nextInt(2, 10)
        _uiState.update {
            it.copy(num1 = n1, num2 = n2, userInput = "", isWrongAnswer = false)
        }
    }

    fun onInputChanged(input: String) {
        val filtered = input.filter { it.isDigit() }
        _uiState.update { it.copy(userInput = filtered, isWrongAnswer = false) }
    }

    fun checkAnswer(): Boolean {
        val state = _uiState.value
        val correctAnswer = state.num1 * state.num2
        val userAnswer = state.userInput.toIntOrNull()
        return if (userAnswer == correctAnswer) {
            true
        } else {
            _uiState.update { it.copy(isWrongAnswer = true, userInput = "") }
            generateNewProblem()
            false
        }
    }
}
