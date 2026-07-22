package com.alan.axolotl.ui.book

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.alan.axolotl.data.BookRepository
import com.alan.axolotl.navigation.BookReaderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BookReaderUiState {
    data object Loading : BookReaderUiState
    data class Success(
        val pages: List<Bitmap>,
        val textByPage: List<String>
    ) : BookReaderUiState
    data object Error : BookReaderUiState
}

/**
 * Holds the reader's UI state. All PDF/Android work lives in [BookRepository],
 * so this class is plain Kotlin and can be unit-tested with a fake repository.
 */
@HiltViewModel
class BookReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // The file name arrives as the type-safe navigation argument.
    private val fileName: String = savedStateHandle.toRoute<BookReaderRoute>().fileName

    private val _uiState = MutableStateFlow<BookReaderUiState>(BookReaderUiState.Loading)
    val uiState: StateFlow<BookReaderUiState> = _uiState.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val content = bookRepository.loadBook(fileName)
            _uiState.value = if (content == null) {
                BookReaderUiState.Error
            } else {
                BookReaderUiState.Success(
                    pages = content.pages,
                    textByPage = content.textByPage
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        (_uiState.value as? BookReaderUiState.Success)?.pages?.forEach { it.recycle() }
    }
}
