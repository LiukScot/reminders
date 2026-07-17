package com.liukscot.reminders.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.liukscot.reminders.RemindersApplication
import kotlinx.coroutines.flow.first
import com.liukscot.reminders.data.SmartList
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import com.liukscot.reminders.ui.screens.ReminderActionButtons
import com.liukscot.reminders.ui.theme.Dimens
import com.liukscot.reminders.ui.navigation.Destination
import com.liukscot.reminders.ui.navigation.FloatingNavBar
import com.liukscot.reminders.ui.screens.ListsScreen
import com.liukscot.reminders.ui.screens.DayScreen
import com.liukscot.reminders.ui.screens.PlaceholderScreen
import com.liukscot.reminders.ui.screens.SearchScreen
import com.liukscot.reminders.ui.screens.SettingsScreen
import com.liukscot.reminders.ui.screens.SmartListScreen
import com.liukscot.reminders.ui.screens.TaskListDetailScreen
import com.liukscot.reminders.ui.screens.WeekScreen

private const val LIST_DETAIL_ROUTE = "listDetail/{listId}"
private fun listDetailRoute(listId: Long) = "listDetail/$listId"
private const val SEARCH_ROUTE = "search"
private const val SMART_LIST_ROUTE = "smartList/{smartList}"
private fun smartListRoute(smartList: SmartList) = "smartList/${smartList.name}"

// Everything gets one horizontal slide, its direction set by where the target sits relative to the
// source: tabs are ordered left-to-right as they appear in the nav bar, and drill-down routes
// (a list's detail, a smart list, search) count as further right than any tab so pushing into one
// always slides in from the right and popping sends it back out. One rule covers tab switches and
// drill-downs alike, which is why no destination needs its own transition override.
private const val DRILL_DOWN_ORDER = 100

private fun routeOrder(route: String?): Int =
    Destination.entries.indexOfFirst { it.route == route }.let { if (it >= 0) it else DRILL_DOWN_ORDER }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.movingForward(): Boolean =
    routeOrder(targetState.destination.route) >= routeOrder(initialState.destination.route)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideEnter() =
    slideInHorizontally(tween(300)) { width -> if (movingForward()) width else -width }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideExit() =
    slideOutHorizontally(tween(300)) { width -> if (movingForward()) -width else width }

@Composable
fun RemindersApp(deepLinkListId: Long? = null) {
    val app = LocalContext.current.applicationContext as RemindersApplication
    // Read the start page once, at launch — it decides where the graph begins, which is fixed for
    // the life of this composition. Changing the setting affects the next launch, not this one.
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val stored = app.settingsRepository.startPageRoute.first()
        startRoute = Destination.entries.firstOrNull { it.route == stored }?.route ?: Destination.Lists.route
    }
    // Blank until the stored page is known — a DataStore read away, so a frame or two at most.
    val resolvedStart = startRoute ?: run {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val navController = rememberNavController()
    // Re-tapping the active tab resets it to today. A tick per tab rather than one shared counter,
    // so resetting Day doesn't also throw away Week's restored scroll position.
    var dayResetTick by remember { mutableIntStateOf(0) }
    var weekResetTick by remember { mutableIntStateOf(0) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val current = Destination.entries.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    // Tapping a due-reminder notification opens straight to that task's list.
    LaunchedEffect(deepLinkListId) {
        if (deepLinkListId != null && deepLinkListId > 0) {
            navController.navigate(listDetailRoute(deepLinkListId))
        }
    }

    val fabState = remember { FabState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CompositionLocalProvider(LocalFabState provides fabState) {
        NavHost(
            navController = navController,
            startDestination = resolvedStart,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            // One directional slide for every move — see slideEnter/slideExit. Deliberately departs
            // from the mockup's instant tab switch, on request.
            enterTransition = { slideEnter() },
            exitTransition = { slideExit() },
            popEnterTransition = { slideEnter() },
            popExitTransition = { slideExit() },
        ) {
            composable(Destination.Lists.route) {
                ListsScreen(
                    onOpenList = { listId -> navController.navigate(listDetailRoute(listId)) },
                    onOpenSmartList = { smartList -> navController.navigate(smartListRoute(smartList)) },
                    onOpenSearch = { navController.navigate(SEARCH_ROUTE) },
                )
            }
            composable(SEARCH_ROUTE) { SearchScreen(onBack = { navController.popBackStack() }) }
            composable(
                route = SMART_LIST_ROUTE,
                arguments = listOf(navArgument("smartList") { type = NavType.StringType }),
            ) { entry ->
                val name = entry.arguments?.getString("smartList") ?: return@composable
                SmartListScreen(
                    smartList = SmartList.valueOf(name),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Settings.route) { SettingsScreen() }
            composable(Destination.Day.route) { DayScreen(resetToTodayTick = dayResetTick) }
            composable(Destination.Week.route) { WeekScreen(resetToTodayTick = weekResetTick) }
            Destination.entries.filter { it != Destination.Lists && it != Destination.Day && it != Destination.Week && it != Destination.Settings }.forEach { destination ->
                composable(destination.route) { PlaceholderScreen(destination.label) }
            }
            composable(
                route = LIST_DETAIL_ROUTE,
                arguments = listOf(navArgument("listId") { type = NavType.LongType }),
            ) { entry ->
                val listId = entry.arguments?.getLong("listId") ?: return@composable
                TaskListDetailScreen(listId = listId, onBack = { navController.popBackStack() })
            }
        }
        }

        // The FAB is drawn once, here, fixed — so it stays put while pages slide underneath instead
        // of swiping off and back on. It clears the nav bar (bottom 88.dp) on the tabs that show one,
        // and sits lower (18.dp) on a drill-down like a list's detail, which has no nav bar.
        fabState.onAdd?.let { onAdd ->
            ReminderActionButtons(
                onAddReminder = onAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = Dimens.screenEdge, bottom = if (current != null) 88.dp else 18.dp),
            )
        }

        if (current != null) {
            FloatingNavBar(
                current = current,
                onSelect = { destination ->
                    if (destination == current) {
                        when (destination) {
                            Destination.Day -> dayResetTick++
                            Destination.Week -> weekResetTick++
                            else -> Unit
                        }
                    }
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}
