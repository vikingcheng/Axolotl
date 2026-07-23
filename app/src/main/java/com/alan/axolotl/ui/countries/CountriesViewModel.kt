package com.alan.axolotl.ui.countries

import androidx.lifecycle.ViewModel
import com.alan.axolotl.data.Continent
import com.alan.axolotl.data.CountryRepository
import com.alan.axolotl.data.SpeechPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** The two kinds of question the quiz can ask. */
enum class QuestionType {
    /** Show a flag, pick the country. */
    FLAG_TO_COUNTRY,

    /** Show a country name, pick the continent. */
    COUNTRY_TO_CONTINENT
}

data class AnswerOption(
    val label: String,
    val isCorrect: Boolean,
    val tapped: Boolean = false
)

data class CountriesUiState(
    val questionType: QuestionType = QuestionType.FLAG_TO_COUNTRY,
    val questionText: String = "",
    /** Big flag shown for [QuestionType.FLAG_TO_COUNTRY]. */
    val promptFlag: String = "",
    /** Country name shown for [QuestionType.COUNTRY_TO_CONTINENT]. */
    val promptName: String = "",
    val options: List<AnswerOption> = emptyList(),
    /** True once the player has committed an answer; further taps are ignored. */
    val answered: Boolean = false,
    val answeredCount: Int = 0,
    val correctCount: Int = 0
)

private const val OPTION_COUNT = 4

@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val countryRepository: CountryRepository,
    private val speechPlayer: SpeechPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(CountriesUiState())
    val uiState: StateFlow<CountriesUiState> = _uiState.asStateFlow()

    private var lastCountryName: String? = null

    init {
        generateQuestion()
    }

    fun generateQuestion() {
        val countries = countryRepository.getCountries()
        // Avoid asking about the same country twice in a row.
        val correct = countries.filter { it.name != lastCountryName }.random()
        lastCountryName = correct.name

        when (QuestionType.entries.random()) {
            QuestionType.FLAG_TO_COUNTRY -> {
                val distractors = countries
                    .filter { it.name != correct.name }
                    .shuffled()
                    .take(OPTION_COUNT - 1)
                val options = (listOf(correct) + distractors)
                    .shuffled()
                    .map { AnswerOption(label = it.name, isCorrect = it.name == correct.name) }

                _uiState.update {
                    it.copy(
                        questionType = QuestionType.FLAG_TO_COUNTRY,
                        questionText = "Which country is this?",
                        promptFlag = correct.flagEmoji,
                        promptName = "",
                        options = options,
                        answered = false
                    )
                }
            }

            QuestionType.COUNTRY_TO_CONTINENT -> {
                val distractors = Continent.entries
                    .filter { it != correct.continent }
                    .shuffled()
                    .take(OPTION_COUNT - 1)
                val options = (listOf(correct.continent) + distractors)
                    .shuffled()
                    .map {
                        AnswerOption(label = it.displayName, isCorrect = it == correct.continent)
                    }

                _uiState.update {
                    it.copy(
                        questionType = QuestionType.COUNTRY_TO_CONTINENT,
                        questionText = "Which continent is this country in?",
                        promptFlag = "",
                        promptName = correct.name,
                        options = options,
                        answered = false
                    )
                }
            }
        }
    }

    /**
     * Handles a tap on an answer button.
     *
     * The option label is always read aloud — for both country and continent
     * answers, and even after the question is locked. Scoring, in contrast, only
     * happens on the first tap: once [CountriesUiState.answered] is true the
     * revealed colours stay put no matter how many more times a button is pressed.
     */
    fun onOptionTapped(index: Int) {
        val state = _uiState.value
        val chosen = state.options.getOrNull(index) ?: return

        speechPlayer.speak(chosen.label)

        if (state.answered) return

        _uiState.update { current ->
            current.copy(
                options = current.options.mapIndexed { i, option ->
                    if (i == index) option.copy(tapped = true) else option
                },
                answered = true,
                answeredCount = current.answeredCount + 1,
                correctCount = current.correctCount + if (chosen.isCorrect) 1 else 0
            )
        }
    }

    /**
     * Reads the question prompt aloud. Only the country-name prompt is spoken —
     * the flag prompt is an emoji with nothing meaningful to pronounce.
     */
    fun onPromptTapped() {
        val state = _uiState.value
        if (state.questionType == QuestionType.COUNTRY_TO_CONTINENT) {
            speechPlayer.speak(state.promptName)
        }
    }

    /** Reads an arbitrary piece of on-screen text aloud (e.g. the question line). */
    fun speak(text: String) {
        speechPlayer.speak(text)
    }

    /** Clears the running score without disturbing the current question. */
    fun resetScore() {
        _uiState.update { it.copy(answeredCount = 0, correctCount = 0) }
    }

    override fun onCleared() {
        super.onCleared()
        speechPlayer.shutdown()
    }
}
