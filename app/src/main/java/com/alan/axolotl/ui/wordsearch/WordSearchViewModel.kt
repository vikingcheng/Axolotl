package com.alan.axolotl.ui.wordsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Direction { HORIZONTAL, VERTICAL }

data class CellPosition(val row: Int, val col: Int)

data class PlacedWord(
    val word: String,
    val start: CellPosition,
    val direction: Direction
) {
    val positions: List<CellPosition>
        get() = word.indices.map { i ->
            when (direction) {
                Direction.HORIZONTAL -> CellPosition(start.row, start.col + i)
                Direction.VERTICAL -> CellPosition(start.row + i, start.col)
            }
        }
}

data class WordSearchUiState(
    val grid: List<List<Char>> = emptyList(),
    val words: List<String> = emptyList(),
    val foundWords: Set<String> = emptySet(),
    val selectedCells: List<CellPosition> = emptyList(),
    val selectionDirection: Direction? = null,
    val flashingCells: Set<CellPosition> = emptySet(),
    val categoryName: String = "",
    val categoryEmoji: String = ""
)

class WordSearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WordSearchUiState())
    val uiState: StateFlow<WordSearchUiState> = _uiState.asStateFlow()

    private var placedWords: List<PlacedWord> = emptyList()

    init {
        generateNewPuzzle()
    }

    fun generateNewPuzzle() {
        val allWords = wordCategories.flatMap { it.words }.shuffled()
        val selectedWords = allWords.take(5)

        val (grid, placed) = generateGrid(selectedWords)

        placedWords = placed
        _uiState.update {
            WordSearchUiState(
                grid = grid,
                words = placed.map { it.word },
                foundWords = emptySet(),
                selectedCells = emptyList(),
                selectionDirection = null,
                flashingCells = emptySet(),
                categoryName = "Mixed",
                categoryEmoji = "\uD83C\uDF1F"
            )
        }
    }

    fun onCellTapped(row: Int, col: Int) {
        val state = _uiState.value
        val tappedCell = CellPosition(row, col)

        if (tappedCell in state.selectedCells) {
            clearSelection()
            return
        }

        val currentSelection = state.selectedCells
        val currentDirection = state.selectionDirection

        if (currentSelection.isEmpty()) {
            _uiState.update { it.copy(selectedCells = listOf(tappedCell), selectionDirection = null) }
            checkForMatch()
            return
        }

        val lastCell = currentSelection.last()

        if (currentSelection.size == 1) {
            val isRight = tappedCell.row == lastCell.row && tappedCell.col == lastCell.col + 1
            val isDown = tappedCell.col == lastCell.col && tappedCell.row == lastCell.row + 1

            if (isRight) {
                _uiState.update {
                    it.copy(
                        selectedCells = currentSelection + tappedCell,
                        selectionDirection = Direction.HORIZONTAL
                    )
                }
                checkForMatch()
            } else if (isDown) {
                _uiState.update {
                    it.copy(
                        selectedCells = currentSelection + tappedCell,
                        selectionDirection = Direction.VERTICAL
                    )
                }
                checkForMatch()
            } else {
                _uiState.update {
                    it.copy(selectedCells = listOf(tappedCell), selectionDirection = null)
                }
                checkForMatch()
            }
            return
        }

        val isValidNext = when (currentDirection) {
            Direction.HORIZONTAL -> tappedCell.row == lastCell.row && tappedCell.col == lastCell.col + 1
            Direction.VERTICAL -> tappedCell.col == lastCell.col && tappedCell.row == lastCell.row + 1
            null -> false
        }

        if (isValidNext) {
            _uiState.update { it.copy(selectedCells = currentSelection + tappedCell) }
            checkForMatch()
        } else {
            _uiState.update {
                it.copy(selectedCells = listOf(tappedCell), selectionDirection = null)
            }
            checkForMatch()
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCells = emptyList(), selectionDirection = null) }
    }

    private fun checkForMatch() {
        val state = _uiState.value
        val selectedLetters = state.selectedCells.map { state.grid[it.row][it.col] }
        val selectedWord = selectedLetters.joinToString("")

        val matchedPlaced = placedWords.find { pw ->
            pw.word == selectedWord && pw.word !in state.foundWords
        }

        if (matchedPlaced != null) {
            val matchedCells = state.selectedCells.toSet()
            _uiState.update {
                it.copy(
                    foundWords = it.foundWords + matchedPlaced.word,
                    flashingCells = matchedCells,
                    selectedCells = emptyList(),
                    selectionDirection = null
                )
            }
            viewModelScope.launch {
                delay(2000)
                _uiState.update { it.copy(flashingCells = emptySet()) }
            }
        }
    }

    private fun generateGrid(words: List<String>): Pair<List<List<Char>>, List<PlacedWord>> {
        val gridSize = 8
        val grid = Array(gridSize) { CharArray(gridSize) { '\u0000' } }
        val placed = mutableListOf<PlacedWord>()

        val sortedWords = words.sortedByDescending { it.length }

        for (word in sortedWords) {
            val success = tryPlaceWord(grid, word, gridSize)
            if (success != null) {
                placed.add(success)
            }
        }

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (grid[r][c] == '\u0000') {
                    grid[r][c] = ('A'..'Z').random()
                }
            }
        }

        return grid.map { it.toList() } to placed
    }

    private fun tryPlaceWord(grid: Array<CharArray>, word: String, gridSize: Int): PlacedWord? {
        val directions = listOf(Direction.HORIZONTAL, Direction.VERTICAL).shuffled()

        for (direction in directions) {
            val positions = generateAllPositions(word.length, direction, gridSize).shuffled()
            for (start in positions) {
                if (canPlace(grid, word, start, direction)) {
                    placeWord(grid, word, start, direction)
                    return PlacedWord(word, start, direction)
                }
            }
        }
        return null
    }

    private fun generateAllPositions(
        wordLength: Int,
        direction: Direction,
        gridSize: Int
    ): List<CellPosition> {
        val positions = mutableListOf<CellPosition>()
        when (direction) {
            Direction.HORIZONTAL -> {
                for (r in 0 until gridSize) {
                    for (c in 0..gridSize - wordLength) {
                        positions.add(CellPosition(r, c))
                    }
                }
            }
            Direction.VERTICAL -> {
                for (r in 0..gridSize - wordLength) {
                    for (c in 0 until gridSize) {
                        positions.add(CellPosition(r, c))
                    }
                }
            }
        }
        return positions
    }

    private fun canPlace(
        grid: Array<CharArray>,
        word: String,
        start: CellPosition,
        direction: Direction
    ): Boolean {
        for (i in word.indices) {
            val (r, c) = when (direction) {
                Direction.HORIZONTAL -> start.row to (start.col + i)
                Direction.VERTICAL -> (start.row + i) to start.col
            }
            val existing = grid[r][c]
            if (existing != '\u0000' && existing != word[i]) {
                return false
            }
        }
        return true
    }

    private fun placeWord(
        grid: Array<CharArray>,
        word: String,
        start: CellPosition,
        direction: Direction
    ) {
        for (i in word.indices) {
            when (direction) {
                Direction.HORIZONTAL -> grid[start.row][start.col + i] = word[i]
                Direction.VERTICAL -> grid[start.row + i][start.col] = word[i]
            }
        }
    }
}
