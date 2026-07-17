package com.alan.axolotl.ui.book

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private const val TAG = "BookReader"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    fileName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayName = fileName.removeSuffix(".pdf")

    val pdfPages = remember(fileName) {
        renderPdfPages(context, fileName)
    }

    val pdfTextByPage = remember(fileName) {
        extractTextFromPdf(context, fileName)
    }

    var ttsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var speakingPageIndex by remember { mutableIntStateOf(-1) }
    val ocrTextCache = remember { mutableStateMapOf<Int, String>() }

    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                speakingPageIndex = -1
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                speakingPageIndex = -1
            }
        })
        tts.value = engine

        onDispose {
            engine.stop()
            engine.shutdown()
            textRecognizer.close()
        }
    }

    DisposableEffect(pdfPages) {
        onDispose {
            pdfPages.forEach { it.recycle() }
        }
    }

    fun doSpeak(pageIndex: Int, text: String) {
        tts.value?.let { engine ->
            engine.setLanguage(Locale.US)
            engine.stop()
            isSpeaking = true
            speakingPageIndex = pageIndex
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "page_$pageIndex")
        }
    }

    fun speakPage(pageIndex: Int) {
        if (!ttsReady) {
            Toast.makeText(context, "Text-to-speech is still loading…", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "speakPage called but TTS not ready yet")
            return
        }

        val pdfText = pdfTextByPage.getOrNull(pageIndex)?.trim()
        if (!pdfText.isNullOrBlank()) {
            Log.d(TAG, "Page $pageIndex PDFBox text: ${pdfText.take(80)}…")
            doSpeak(pageIndex, pdfText)
            return
        }

        Log.d(TAG, "Page $pageIndex: no PDFBox text, trying OCR")

        val cachedOcr = ocrTextCache[pageIndex]
        if (cachedOcr != null) {
            if (cachedOcr.isBlank()) {
                Toast.makeText(context, "No readable text found on this page", Toast.LENGTH_SHORT).show()
            } else {
                doSpeak(pageIndex, cachedOcr)
            }
            return
        }

        val bitmap = pdfPages.getOrNull(pageIndex)
        if (bitmap == null) {
            Toast.makeText(context, "Page not available", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Scanning page for text…", Toast.LENGTH_SHORT).show()

        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                val ocrText = result.text.trim()
                ocrTextCache[pageIndex] = ocrText
                Log.d(TAG, "Page $pageIndex OCR result: ${ocrText.take(80)}…")
                if (ocrText.isBlank()) {
                    Toast.makeText(context, "No readable text found on this page", Toast.LENGTH_SHORT).show()
                } else {
                    doSpeak(pageIndex, ocrText)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed for page $pageIndex", e)
                ocrTextCache[pageIndex] = ""
                Toast.makeText(context, "Could not read text from this page", Toast.LENGTH_SHORT).show()
            }
    }

    fun stopSpeaking() {
        tts.value?.stop()
        isSpeaking = false
        speakingPageIndex = -1
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val listState = rememberLazyListState()

    val currentVisiblePage by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        stopSpeaking()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Row {
                        IconButton(onClick = {
                            scale = (scale - 0.25f).coerceIn(0.5f, 5f)
                            if (scale <= 1f) offset = Offset.Zero
                        }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
                        }
                        IconButton(onClick = {
                            scale = (scale + 0.25f).coerceIn(0.5f, 5f)
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Zoom in")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (pdfPages.isNotEmpty()) {
                SpeakerFab(
                    isSpeaking = isSpeaking,
                    onTap = {
                        if (isSpeaking) {
                            stopSpeaking()
                        } else {
                            speakPage(currentVisiblePage)
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (pdfPages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Could not open book",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF2C2C2C))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(0.5f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                val maxX = (size.width * (newScale - 1f)) / 2f
                                val maxY = (size.height * (newScale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x * newScale).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y * newScale).coerceIn(-maxY, maxY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentPadding = PaddingValues(
                        horizontal = if (scale < 1f) (((1f - scale) / 2f) * 1000).dp.coerceAtMost(120.dp) else 0.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState,
                    userScrollEnabled = scale <= 1.01f
                ) {
                    itemsIndexed(pdfPages) { index, bitmap ->
                        val isThisPageSpeaking = speakingPageIndex == index
                        Box {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Book page ${index + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                if (isSpeaking && speakingPageIndex == index) {
                                                    stopSpeaking()
                                                } else {
                                                    speakPage(index)
                                                }
                                            }
                                        )
                                    },
                                contentScale = ContentScale.FillWidth
                            )
                            if (isThisPageSpeaking) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                            shape = CircleShape
                                        )
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speaking this page",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeakerFab(
    isSpeaking: Boolean,
    onTap: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaker_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    FloatingActionButton(
        onClick = onTap,
        containerColor = if (isSpeaking)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isSpeaking)
            MaterialTheme.colorScheme.onError
        else
            MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
        modifier = Modifier
            .size(72.dp)
            .then(if (isSpeaking) Modifier.scale(pulseScale) else Modifier)
    ) {
        Icon(
            imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isSpeaking) "Stop reading" else "Read this page",
            modifier = Modifier.size(36.dp)
        )
    }
}

private fun renderPdfPages(context: android.content.Context, fileName: String): List<Bitmap> {
    val pages = mutableListOf<Bitmap>()
    try {
        val tempFile = File(context.cacheDir, fileName)
        context.assets.open("books/$fileName").use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        val fileDescriptor = android.os.ParcelFileDescriptor.open(
            tempFile,
            android.os.ParcelFileDescriptor.MODE_READ_ONLY
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

private fun extractTextFromPdf(context: android.content.Context, fileName: String): List<String> {
    val textPages = mutableListOf<String>()
    try {
        PDFBoxResourceLoader.init(context)
        val tempFile = File(context.cacheDir, fileName)
        if (!tempFile.exists()) {
            context.assets.open("books/$fileName").use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        val document = PDDocument.load(tempFile)
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
