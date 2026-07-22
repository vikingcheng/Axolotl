package com.alan.axolotl.ui.read

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import com.alan.axolotl.data.SentenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject

data class WordComparison(
    val word: String,
    val isCorrect: Boolean
)

data class ReadUiState(
    val currentSentence: String = "",
    val words: List<String> = emptyList(),
    val tappedWordIndices: Set<Int> = emptySet(),
    val difficulty: Difficulty = Difficulty.EASY,
    val sentenceIndex: Int = 0,
    val isListening: Boolean = false,
    val starsEarned: Int = -1,
    val maxStars: Int = 5,
    val recognizedText: String = "",
    val wordComparisons: List<WordComparison> = emptyList(),
    val correctWordCount: Int = 0,
    val totalWordCount: Int = 0,
    val showResult: Boolean = false,
    val errorMessage: String? = null,
    val speechAvailable: Boolean = true
)

@HiltViewModel
class ReadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sentenceRepository: SentenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadUiState())
    val uiState: StateFlow<ReadUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.language = Locale.US
        }

        val available = SpeechRecognizer.isRecognitionAvailable(context)
        _uiState.update { it.copy(speechAvailable = available) }

        if (available) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        }

        loadSentence()
    }

    private fun loadSentence() {
        val state = _uiState.value
        val levels = sentenceRepository.getLevels()
        val level = levels.find { it.difficulty == state.difficulty } ?: levels[0]
        val sentence = level.sentences[state.sentenceIndex % level.sentences.size]
        val words = sentence.split(" ")

        _uiState.update {
            it.copy(
                currentSentence = sentence,
                words = words,
                tappedWordIndices = emptySet(),
                starsEarned = -1,
                recognizedText = "",
                wordComparisons = emptyList(),
                correctWordCount = 0,
                totalWordCount = 0,
                showResult = false,
                errorMessage = null,
                isListening = false
            )
        }
    }

    fun onWordTapped(index: Int) {
        val state = _uiState.value
        if (state.showResult || state.isListening) return

        _uiState.update { it.copy(tappedWordIndices = it.tappedWordIndices + index) }

        if (ttsReady) {
            val word = state.words[index].replace(Regex("[^a-zA-Z']"), "")
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_$index")
        }
    }

    fun startListening() {
        val state = _uiState.value
        if (!state.speechAvailable || state.isListening || state.showResult) return

        _uiState.update { it.copy(isListening = true, errorMessage = null) }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _uiState.update { it.copy(isListening = false) }
        }

        override fun onError(error: Int) {
            _uiState.update {
                it.copy(
                    isListening = false,
                    errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't hear anything. Try again!"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Try again!"
                        SpeechRecognizer.ERROR_AUDIO -> "Microphone problem. Try again!"
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue. Try again!"
                        else -> "Something went wrong. Try again!"
                    }
                )
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestMatch = matches?.firstOrNull() ?: ""
            processRecognitionResult(bestMatch)
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processRecognitionResult(recognizedText: String) {
        val state = _uiState.value
        val originalWords = state.currentSentence.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val recognizedWords = recognizedText.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val comparisons = mutableListOf<WordComparison>()
        var correctCount = 0

        for (i in recognizedWords.indices) {
            val recognized = recognizedWords[i]
            val isCorrect = i < originalWords.size &&
                    normalizeWord(recognized) == normalizeWord(originalWords[i])
            comparisons.add(WordComparison(word = recognized, isCorrect = isCorrect))
            if (isCorrect) correctCount++
        }

        // If recognized has fewer words than original, missing ones count as wrong
        val totalWords = originalWords.size
        val matchRatio = if (totalWords > 0) correctCount.toFloat() / totalWords else 0f
        val stars = kotlin.math.round(matchRatio * 5).toInt().coerceIn(0, 5)

        _uiState.update {
            it.copy(
                recognizedText = recognizedText,
                wordComparisons = comparisons,
                correctWordCount = correctCount,
                totalWordCount = totalWords,
                starsEarned = stars,
                showResult = true,
                isListening = false
            )
        }
    }

    private fun normalizeWord(word: String): String {
        return word.lowercase().replace(Regex("[^a-z']"), "")
    }

    fun nextSentence() {
        val state = _uiState.value
        val levels = sentenceRepository.getLevels()
        val level = levels.find { it.difficulty == state.difficulty } ?: levels[0]
        val nextIndex = (state.sentenceIndex + 1) % level.sentences.size

        _uiState.update { it.copy(sentenceIndex = nextIndex) }
        loadSentence()
    }

    fun changeDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty, sentenceIndex = 0) }
        loadSentence()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun tryAgain() {
        _uiState.update {
            it.copy(
                starsEarned = -1,
                recognizedText = "",
                wordComparisons = emptyList(),
                correctWordCount = 0,
                totalWordCount = 0,
                showResult = false,
                errorMessage = null,
                tappedWordIndices = emptySet()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
