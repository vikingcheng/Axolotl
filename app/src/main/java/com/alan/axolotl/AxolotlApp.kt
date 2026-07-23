package com.alan.axolotl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.alan.axolotl.navigation.BookReaderRoute
import com.alan.axolotl.navigation.BookRoute
import com.alan.axolotl.navigation.CountriesRoute
import com.alan.axolotl.navigation.HomeRoute
import com.alan.axolotl.navigation.LockRoute
import com.alan.axolotl.navigation.PasswordGateRoute
import com.alan.axolotl.navigation.ProfileRoute
import com.alan.axolotl.navigation.ReadRoute
import com.alan.axolotl.navigation.TimerRoute
import com.alan.axolotl.navigation.TopLevelDestination
import com.alan.axolotl.navigation.WordSearchRoute
import com.alan.axolotl.ui.book.BookReaderScreen
import com.alan.axolotl.ui.book.BookScreen
import com.alan.axolotl.ui.countries.CountriesScreen
import com.alan.axolotl.ui.home.HomeScreen
import com.alan.axolotl.ui.lock.LockScreen
import com.alan.axolotl.ui.profile.PasswordGateScreen
import com.alan.axolotl.ui.profile.ProfileScreen
import com.alan.axolotl.ui.read.ReadScreen
import com.alan.axolotl.ui.timer.TimerScreen
import com.alan.axolotl.ui.wordsearch.WordSearchScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Where a feature actually navigates to. Timer is gated behind the parental
 * password screen, everything else goes straight to its own route.
 */
private val TopLevelDestination.navigationRoute: Any
    get() = if (this == TopLevelDestination.TIMER) PasswordGateRoute else route

/** Standard top-level navigation: reset to the start destination, keeping saved state. */
private fun NavController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** True when [destination] is the tab currently being shown. */
private fun NavDestination?.isCurrentTab(destination: TopLevelDestination): Boolean =
    if (destination == TopLevelDestination.TIMER) {
        // The password gate counts as "on the Timer tab" so the highlight doesn't flicker.
        this?.hasRoute<TimerRoute>() == true || this?.hasRoute<PasswordGateRoute>() == true
    } else {
        this?.hasRoute(destination.route::class) == true
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxolotlApp(
    showLockFlow: StateFlow<Boolean>,
    onLockConsumed: () -> Unit = {},
    onLockEngaged: () -> Unit = {},
    onLockDisengaged: () -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isOnLockScreen = currentDestination?.hasRoute<LockRoute>() == true
    val isOnBookReader = currentDestination?.hasRoute<BookReaderRoute>() == true
    val showChrome = !isOnLockScreen && !isOnBookReader

    val showLock by showLockFlow.collectAsState()
    LaunchedEffect(showLock) {
        if (showLock) {
            onLockConsumed()
            if (currentDestination?.hasRoute<LockRoute>() != true) {
                onLockEngaged()
                navController.navigate(LockRoute) {
                    launchSingleTop = true
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showChrome,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "\uD83E\uDD8E " + stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(28.dp)
                )
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination.isCurrentTab(destination)
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destination.labelRes),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigateToTab(destination.navigationRoute)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))

                val profileSelected = currentDestination?.hasRoute<ProfileRoute>() == true
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = stringResource(R.string.drawer_profile)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.drawer_profile),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    selected = profileSelected,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateToTab(ProfileRoute)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination.isCurrentTab(destination)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigateToTab(destination.navigationRoute)
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = stringResource(destination.labelRes)
                                    )
                                },
                                label = { Text(stringResource(destination.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = if (showChrome) Modifier.padding(innerPadding) else Modifier
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onFeatureClick = { feature ->
                            navController.navigateToTab(feature.navigationRoute)
                        }
                    )
                }
                composable<PasswordGateRoute> {
                    PasswordGateScreen(
                        onPasswordCorrect = {
                            navController.navigate(TimerRoute) {
                                popUpTo<PasswordGateRoute> { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable<ProfileRoute> {
                    ProfileScreen()
                }
                composable<TimerRoute> {
                    TimerScreen(
                        onTimerFinished = {
                            onLockEngaged()
                            navController.navigate(LockRoute) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable<LockRoute> {
                    LockScreen(
                        onUnlocked = {
                            onLockDisengaged()
                            navController.popBackStack(LockRoute, inclusive = true)
                        }
                    )
                }
                composable<BookRoute> {
                    BookScreen(
                        onBookClick = { fileName ->
                            navController.navigate(BookReaderRoute(fileName = fileName)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable<CountriesRoute> {
                    CountriesScreen()
                }
                composable<WordSearchRoute> {
                    WordSearchScreen()
                }
                composable<ReadRoute> {
                    ReadScreen()
                }
                composable<BookReaderRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<BookReaderRoute>()
                    BookReaderScreen(
                        fileName = route.fileName,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
