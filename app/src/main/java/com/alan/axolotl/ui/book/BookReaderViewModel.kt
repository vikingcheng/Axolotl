package com.alan.axolotl.ui.book

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "BookReader"

sealed interface BookReaderUiState {
    data object Loading : BookReaderUiState
    data class Success(
        val pages: List<Bitmap>,
        val textByPage: List<String>
    ) : BookReaderUiState
    data object Error : BookReaderUiState
}

class BookReaderViewModel(
    application: Application,
    private val fileName: String
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BookReaderUiState>(BookReaderUiState.Loading)
    val uiState: StateFlow<BookReaderUiState> = _uiState.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                // Extract the asset to a cache file exactly once, then reuse it for
                // both rendering and text extraction to avoid a write race.
                val tempFile = File(context.cacheDir, fileName)
                try {
                    context.assets.open("books/$fileName").use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy book asset $fileName", e)
                    return@withContext BookReaderUiState.Error
                }

                val pages = renderPdfPages(tempFile)
                if (pages.isEmpty()) {
                    return@withContext BookReaderUiState.Error
                }
                val text = extractTextFromPdf(context, tempFile)
                BookReaderUiState.Success(pages, text)
            }
            _uiState.value = result
        }
    }

    private fun renderPdfPages(file: File): List<Bitmap> {
        val pages = mutableListOf<Bitmap>()
        try {
            val fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(fileDescriptor)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val renderScale = 3
                val bitmap = Bitmap.createBitmap(
                    page.width * renderScale,
                    page.height * renderScale,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pages.add(bitmap)
                page.close()
            }

            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render PDF pages", e)
        }
        return pages
    }

    private fun extractTextFromPdf(context: android.content.Context, file: File): List<String> {
        val textPages = mutableListOf<String>()
        try {
            PDFBoxResourceLoader.init(context)
            val document = PDDocument.load(file)
            val stripper = PDFTextStripper()

            for (i in 1..document.numberOfPages) {
                stripper.startPage = i
                stripper.endPage = i
                val text = stripper.getText(document)
                textPages.add(text)
                Log.d(TAG, "PDFBox page $i text (${text.trim().length} chars): ${text.trim().take(80)}")
            }

            document.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract text from PDF", e)
        }
        return textPages
    }

    override fun onCleared() {
        super.onCleared()
        (_uiState.value as? BookReaderUiState.Success)?.pages?.forEach { it.recycle() }
    }

    companion object {
        fun factory(fileName: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                    return BookReaderViewModel(application, fileName) as T
                }
            }
    }
}
