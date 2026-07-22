package com.alan.axolotl.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val TAG = "BookRepository"

/** A fully loaded book: one rendered bitmap per page, plus any extractable text layer. */
data class BookContent(
    val pages: List<Bitmap>,
    val textByPage: List<String>
)

/**
 * Loads books shipped in `assets/books`.
 *
 * This is the only place that knows about Context, PdfRenderer and PDFBox, which
 * keeps [com.alan.axolotl.ui.book.BookReaderViewModel] free of Android
 * framework dependencies (and therefore unit-testable with a fake).
 */
interface BookRepository {
    /**
     * Renders [fileName] into bitmaps and extracts its text layer.
     * Returns null when the book cannot be opened. Safe to call from any dispatcher.
     */
    suspend fun loadBook(fileName: String): BookContent?
}

class PdfBookRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : BookRepository {

    override suspend fun loadBook(fileName: String): BookContent? = withContext(Dispatchers.IO) {
        // Extract the asset to a cache file exactly once, then reuse it for both
        // rendering and text extraction to avoid a write race.
        val tempFile = File(context.cacheDir, fileName)
        try {
            context.assets.open("books/$fileName").use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy book asset $fileName", e)
            return@withContext null
        }

        val pages = renderPdfPages(tempFile)
        if (pages.isEmpty()) {
            return@withContext null
        }
        BookContent(pages = pages, textByPage = extractTextFromPdf(tempFile))
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

    private fun extractTextFromPdf(file: File): List<String> {
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
}
