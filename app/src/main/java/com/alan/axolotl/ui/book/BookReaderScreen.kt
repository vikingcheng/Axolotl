package com.alan.axolotl.ui.book

import android.graphics.Bitmap
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alan.axolotl.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    val viewModel: BookReaderViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        BookReaderUiState.Loading -> {
            BookReaderLoading(displayName = displayName, onBack = onBack, modifier = modifier)
            return
        }
        BookReaderUiState.Error -> {
            BookReaderError(displayName = displayName, onBack = onBack, modifier = modifier)
            return
        }
        is BookReaderUiState.Success -> {
            BookReaderContent(
                displayName = displayName,
                pdfPages = state.pages,
                pdfTextByPage = state.textByPage,
                onBack = onBack,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookReaderContent(
    displayName: String,
    pdfPages: List<Bitmap>,
    pdfTextByPage: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            Toast.makeText(context, context.getString(R.string.book_reader_tts_loading), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, context.getString(R.string.book_reader_no_text), Toast.LENGTH_SHORT).show()
            } else {
                doSpeak(pageIndex, cachedOcr)
            }
            return
        }

        val bitmap = pdfPages.getOrNull(pageIndex)
        if (bitmap == null) {
            Toast.makeText(context, context.getString(R.string.book_reader_page_unavailable), Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, context.getString(R.string.book_reader_scanning), Toast.LENGTH_SHORT).show()

        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                val ocrText = result.text.trim()
                ocrTextCache[pageIndex] = ocrText
                Log.d(TAG, "Page $pageIndex OCR result: ${ocrText.take(80)}…")
                if (ocrText.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.book_reader_no_text), Toast.LENGTH_SHORT).show()
                } else {
                    doSpeak(pageIndex, ocrText)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed for page $pageIndex", e)
                ocrTextCache[pageIndex] = ""
                Toast.makeText(context, context.getString(R.string.book_reader_read_failed), Toast.LENGTH_SHORT).show()
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
                            contentDescription = stringResource(R.string.book_reader_back)
                        )
                    }
                },
                actions = {
                    Row {
                        IconButton(onClick = {
                            scale = (scale - 0.25f).coerceIn(0.5f, 5f)
                            if (scale <= 1f) offset = Offset.Zero
                        }) {
                            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.book_reader_zoom_out))
                        }
                        IconButton(onClick = {
                            scale = (scale + 0.25f).coerceIn(0.5f, 5f)
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.book_reader_zoom_in))
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
                    text = stringResource(R.string.book_reader_could_not_open),
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
                                contentDescription = stringResource(R.string.book_reader_page_description, index + 1),
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
                                        contentDescription = stringResource(R.string.book_reader_speaking_page),
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
            contentDescription = if (isSpeaking) stringResource(R.string.book_reader_stop_reading) else stringResource(R.string.book_reader_read_this_page),
            modifier = Modifier.size(36.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookReaderScaffold(
    displayName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
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
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.book_reader_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier,
        content = content
    )
}

@Composable
private fun BookReaderLoading(
    displayName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BookReaderScaffold(displayName = displayName, onBack = onBack, modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun BookReaderError(
    displayName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BookReaderScaffold(displayName = displayName, onBack = onBack, modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.book_reader_could_not_open),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
