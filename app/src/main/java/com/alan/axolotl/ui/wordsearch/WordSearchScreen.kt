package com.alan.axolotl.ui.wordsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.alan.axolotl.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

private val FoundGreen = Color(0xFF4CAF50)
private val SelectedBlue = Color(0xFFBBDEFB)
private val FlashGreen = Color(0xFFA5D6A7)
private val GridBorder = Color(0xFFBDBDBD)

@Composable
fun WordSearchScreen(
    viewModel: WordSearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.grid.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        for (row in 0 until 8) {
                            Row {
                                for (col in 0 until 8) {
                                    val cellPos = CellPosition(row, col)
                                    val isSelected = cellPos in uiState.selectedCells
                                    val isFlashing = cellPos in uiState.flashingCells

                                    val bgColor = when {
                                        isFlashing -> FlashGreen
                                        isSelected -> SelectedBlue
                                        else -> Color.Transparent
                                    }

                                    val textColor = when {
                                        isFlashing -> FoundGreen
                                        isSelected -> Color(0xFF1565C0)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(bgColor)
                                            .border(
                                                width = 0.5.dp,
                                                color = GridBorder,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { viewModel.onCellTapped(row, col) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = uiState.grid[row][col].toString(),
                                            fontSize = 43.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.word_search_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${uiState.categoryEmoji} ${uiState.categoryName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.word_search_words_to_find),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            uiState.words.forEach { word ->
                val isFound = word in uiState.foundWords
                Box(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFound) FoundGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isFound) FoundGreen else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isFound) {
                            Text(
                                text = "\u2705",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = word,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFound) FoundGreen else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isFound) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.selectedCells.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearSelection() }) {
                    Text(
                        text = stringResource(R.string.word_search_clear_selection),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState.foundWords.size == uiState.words.size && uiState.words.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.word_search_all_found),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = FoundGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.generateNewPuzzle() },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.word_search_next),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
