package com.alan.axolotl.ui.countries

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AnswerOption(
    val country: Country,
    val tapped: Boolean = false,
    val isCorrect: Boolean = false
)

data class CountriesUiState(
    val correctCountry: Country = allCountries.first(),
    val options: List<AnswerOption> = emptyList(),
    val answered: Boolean = false
)

class CountriesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CountriesUiState())
    val uiState: StateFlow<CountriesUiState> = _uiState.asStateFlow()

    private var lastCountryName: String? = null

    init {
        generateQuestion()
    }

    fun generateQuestion() {
        val available = allCountries.filter { it.name != lastCountryName }
        val correct = available.random()
        lastCountryName = correct.name

        val distractors = allCountries
            .filter { it.name != correct.name }
            .shuffled()
            .take(3)

        val options = (listOf(correct) + distractors)
            .shuffled()
            .map { AnswerOption(country = it, isCorrect = it.name == correct.name) }

        _uiState.update {
            CountriesUiState(
                correctCountry = correct,
                options = options,
                answered = false
            )
        }
    }

    fun onOptionTapped(index: Int) {
        _uiState.update { state ->
            val updatedOptions = state.options.toMutableList()
            updatedOptions[index] = updatedOptions[index].copy(tapped = true)
            val answered = updatedOptions[index].isCorrect || state.answered
            state.copy(options = updatedOptions, answered = answered)
        }
    }
}
