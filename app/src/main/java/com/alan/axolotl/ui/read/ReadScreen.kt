package com.alan.axolotl.ui.read

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadScreen(
    viewModel: ReadViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DifficultySelector(
            currentDifficulty = uiState.difficulty,
            onDifficultyChange = { viewModel.changeDifficulty(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        StarDisplay(
            starsEarned = uiState.starsEarned,
            maxStars = uiState.maxStars
        )

        Spacer(modifier = Modifier.height(24.dp))

        SentenceDisplay(
            words = uiState.words,
            tappedIndices = uiState.tappedWordIndices,
            onWordTapped = { index -> viewModel.onWordTapped(index) },
            enabled = !uiState.showResult && !uiState.isListening
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!uiState.showResult) {
            Text(
                text = "Tap a word to hear it \uD83D\uDD0A",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!uiState.speechAvailable) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Speech recognition is not available on this device.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        } else if (!hasAudioPermission) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Allow Microphone")
            }
        } else if (uiState.showResult) {
            ResultDisplay(
                starsEarned = uiState.starsEarned,
                maxStars = uiState.maxStars,
                wordComparisons = uiState.wordComparisons,
                correctWordCount = uiState.correctWordCount,
                totalWordCount = uiState.totalWordCount,
                onNext = { viewModel.nextSentence() },
                onTryAgain = { viewModel.tryAgain() }
            )
        } else {
            MicrophoneButton(
                isListening = uiState.isListening,
                onClick = {
                    if (uiState.isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
            )

            if (uiState.isListening) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Listening... \uD83D\uDC42",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DifficultySelector(
    currentDifficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Level:",
            style = MaterialTheme.typography.titleMedium
        )
        Difficulty.entries.forEach { difficulty ->
            FilterChip(
                selected = difficulty == currentDifficulty,
                onClick = { onDifficultyChange(difficulty) },
                label = {
                    Text(
                        text = difficulty.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@Composable
private fun StarDisplay(starsEarned: Int, maxStars: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (i in 1..maxStars) {
            val filled = starsEarned >= i
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (filled) Color(0xFFFFD700) else Color(0xFFBDBDBD),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceDisplay(
    words: List<String>,
    tappedIndices: Set<Int>,
    onWordTapped: (Int) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEachIndexed { index, word ->
                WordChip(
                    word = word,
                    isTapped = index in tappedIndices,
                    onClick = { if (enabled) onWordTapped(index) }
                )
                if (index < words.size - 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun WordChip(
    word: String,
    isTapped: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isTapped) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            Color.Transparent
        },
        label = "wordBg"
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = if (isTapped) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isTapped) 2.dp else 0.dp
        )
    ) {
        Text(
            text = word,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MicrophoneButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Box(contentAlignment = Alignment.Center) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(80.dp)
                .scale(if (isListening) scale else 1f),
            shape = CircleShape,
            containerColor = if (isListening) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (isListening) {
                MaterialTheme.colorScheme.onError
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start reading",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultDisplay(
    starsEarned: Int,
    maxStars: Int,
    wordComparisons: List<WordComparison>,
    correctWordCount: Int,
    totalWordCount: Int,
    onNext: () -> Unit,
    onTryAgain: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (wordComparisons.isNotEmpty()) {
            Text(
                text = "What I heard:",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wordComparisons.forEachIndexed { index, comparison ->
                        Text(
                            text = comparison.word,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (comparison.isCorrect) {
                                Color(0xFF2E7D32)
                            } else {
                                Color(0xFFC62828)
                            }
                        )
                        if (index < wordComparisons.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }

            Text(
                text = "$correctWordCount / $totalWordCount words correct",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 1..maxStars) {
                val filled = starsEarned >= i
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (filled) Color(0xFFFFD700) else Color(0xFFBDBDBD),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        val message = when {
            starsEarned >= 5 -> "Amazing! Perfect! \uD83C\uDF1F"
            starsEarned >= 4 -> "Great job! \uD83C\uDF89"
            starsEarned >= 3 -> "Good reading! \uD83D\uDC4D"
            starsEarned >= 2 -> "Nice try! Keep going! \uD83D\uDCAA"
            starsEarned >= 1 -> "Good effort! Try again? \uD83D\uDE0A"
            else -> "Let's try again! You can do it! \uD83D\uDC4B"
        }

        Text(
            text = message,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onTryAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Filled.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Again", fontSize = 18.sp)
            }

            Button(onClick = onNext) {
                Text("Next", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Filled.NavigateNext, contentDescription = null)
            }
        }
    }
}
