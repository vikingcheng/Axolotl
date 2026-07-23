package com.alan.axolotl.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alan.axolotl.R
import com.alan.axolotl.navigation.DestinationAccent
import com.alan.axolotl.navigation.TopLevelDestination

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onFeatureClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "🦎",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_prompt),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 5
        ) {
            TopLevelDestination.homeFeatures.forEach { feature ->
                FeatureTile(
                    feature = feature,
                    onClick = { onFeatureClick(feature) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureTile(
    feature: TopLevelDestination,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tileContainerColor(feature.accent),
            contentColor = tileContentColor(feature.accent)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = feature.selectedIcon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(feature.labelRes),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun tileContainerColor(accent: DestinationAccent): Color = when (accent) {
    DestinationAccent.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
    DestinationAccent.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
    DestinationAccent.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
private fun tileContentColor(accent: DestinationAccent): Color = when (accent) {
    DestinationAccent.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
    DestinationAccent.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
    DestinationAccent.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
}
