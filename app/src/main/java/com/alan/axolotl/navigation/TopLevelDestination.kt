package com.alan.axolotl.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/** Which Material colour role a feature's Home-screen tile is painted with. */
enum class DestinationAccent { PRIMARY, SECONDARY, TERTIARY }

/**
 * The single source of truth for the app's features.
 *
 * Everything that presents the feature list — the navigation drawer, the bottom
 * bar and the Home screen tiles — iterates over this enum, so adding a feature
 * means adding one entry here (plus its `composable<Route>` in the NavHost).
 */
enum class TopLevelDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val accent: DestinationAccent
) {
    HOME(
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = HomeRoute,
        accent = DestinationAccent.PRIMARY
    ),
    TIMER(
        label = "Timer",
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer,
        route = TimerRoute,
        accent = DestinationAccent.PRIMARY
    ),
    BOOK(
        label = "Books",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        route = BookRoute,
        accent = DestinationAccent.SECONDARY
    ),
    COUNTRIES(
        label = "Countries",
        selectedIcon = Icons.Filled.Public,
        unselectedIcon = Icons.Outlined.Public,
        route = CountriesRoute,
        accent = DestinationAccent.TERTIARY
    ),
    WORD_SEARCH(
        label = "Word Search",
        selectedIcon = Icons.Filled.GridOn,
        unselectedIcon = Icons.Outlined.GridOn,
        route = WordSearchRoute,
        accent = DestinationAccent.PRIMARY
    ),
    READ(
        label = "Read",
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.Mic,
        route = ReadRoute,
        accent = DestinationAccent.SECONDARY
    );

    companion object {
        /** The features that get a tile on the Home screen — everything except Home itself. */
        val homeFeatures: List<TopLevelDestination> = entries.filter { it != HOME }
    }
}
