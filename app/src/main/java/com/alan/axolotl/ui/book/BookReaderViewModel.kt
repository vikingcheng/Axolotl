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

    /**
     * Releases the rendered page bitmaps once this ViewModel is destroyed for good.
     *
     * Pages are rendered at 3x scale in [Bitmap.Config.ARGB_8888], so a single book can
     * hold hundreds of megabytes of native pixel memory. On API 26+ that memory is
     * reclaimed by the garbage collector eventually, but "eventually" is not good enough
     * here: opening a second book before a collection runs would double the peak
     * footprint. [Bitmap.recycle] frees it deterministically instead.
     *
     * [onCleared] is the right hook because it fires only when the ViewModel's scope is
     * gone for good — notably *not* on configuration changes, where the bitmaps must
     * survive so the recreated UI can keep drawing them from [uiState].
     */
    override fun onCleared() {
        super.onCleared()
        (_uiState.value as? BookReaderUiState.Success)?.pages?.forEach { it.recycle() }
    }
}
