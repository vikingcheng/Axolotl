package com.alan.axolotl.ui.countries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

private val CorrectGreen = Color(0xFF4CAF50)
private val WrongRed = Color(0xFFE53935)

@Composable
fun CountriesScreen(
    viewModel: CountriesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        ScoreBoard(
            answeredCount = uiState.answeredCount,
            correctCount = uiState.correctCount,
            onReset = viewModel::resetScore,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.questionText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            QuestionPrompt(
                questionType = uiState.questionType,
                promptFlag = uiState.promptFlag,
                promptName = uiState.promptName,
                onPromptTapped = viewModel::onPromptTapped
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnswerGrid(
                options = uiState.options,
                answered = uiState.answered,
                onOptionTapped = viewModel::onOptionTapped
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Button(
                    onClick = viewModel::generateQuestion,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "Next ➡️",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Running tally in the top-left corner, with a button to start counting over. */
@Composable
private fun ScoreBoard(
    answeredCount: Int,
    correctCount: Int,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Answered: $answeredCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Correct: $correctCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        TextButton(
            onClick = onReset,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * The thing being asked about: a big flag, or a country name.
 *
 * The country name is tappable so the child can hear it pronounced.
 */
@Composable
private fun QuestionPrompt(
    questionType: QuestionType,
    promptFlag: String,
    promptName: String,
    onPromptTapped: () -> Unit
) {
    when (questionType) {
        QuestionType.FLAG_TO_COUNTRY -> Text(
            text = promptFlag,
            fontSize = 180.sp,
            textAlign = TextAlign.Center
        )

        QuestionType.COUNTRY_TO_CONTINENT -> Text(
            text = promptName,
            fontSize = 76.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(
                onClickLabel = "Say this country name"
            ) { onPromptTapped() }
        )
    }
}

@Composable
private fun AnswerGrid(
    options: List<AnswerOption>,
    answered: Boolean,
    onOptionTapped: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (rowStart in options.indices step 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in rowStart until minOf(rowStart + 2, options.size)) {
                    AnswerButton(
                        option = options[i],
                        answered = answered,
                        onClick = { onOptionTapped(i) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerButton(
    option: AnswerOption,
    answered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Before answering everything is neutral. Once an answer is committed the
    // correct option always turns green, and a wrong pick turns red — the other
    // wrong options stay neutral so the reveal is easy to read.
    val containerColor = when {
        !answered -> MaterialTheme.colorScheme.surfaceVariant
        option.isCorrect -> CorrectGreen
        option.tapped -> WrongRed
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        !answered -> MaterialTheme.colorScheme.onSurfaceVariant
        option.isCorrect || option.tapped -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = option.label,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
