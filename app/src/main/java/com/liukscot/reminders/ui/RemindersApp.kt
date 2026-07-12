package com.liukscot.reminders.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.liukscot.reminders.ui.navigation.Destination
import com.liukscot.reminders.ui.navigation.FloatingNavBar
import com.liukscot.reminders.ui.screens.ListsScreen
import com.liukscot.reminders.ui.screens.PlaceholderScreen
import com.liukscot.reminders.ui.screens.SettingsScreen
import com.liukscot.reminders.ui.screens.TaskListDetailScreen

private const val LIST_DETAIL_ROUTE = "listDetail/{listId}"
private fun listDetailRoute(listId: Long) = "listDetail/$listId"

@Composable
fun RemindersApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val current = Destination.entries.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = Destination.Lists.route,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            // Mockup switches screens instantly (only sheets/toggles animate) —
            // override Navigation Compose's built-in 700ms crossfade default.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(
                Destination.Lists.route,
                // Lists participates in two different transitions: an
                // instant tab switch (Day/Week/Settings) and a push into the
                // list detail route. Only the latter should slide, so pick
                // the animation based on which destination is on the other
                // end of the transition.
                exitTransition = {
                    if (targetState.destination.route == LIST_DETAIL_ROUTE) {
                        slideOutHorizontally(tween(300)) { -it }
                    } else {
                        ExitTransition.None
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route == LIST_DETAIL_ROUTE) {
                        slideInHorizontally(tween(300)) { -it }
                    } else {
                        EnterTransition.None
                    }
                },
            ) {
                ListsScreen(onOpenList = { listId -> navController.navigate(listDetailRoute(listId)) })
            }
            composable(Destination.Settings.route) { SettingsScreen() }
            Destination.entries.filter { it != Destination.Lists && it != Destination.Settings }.forEach { destination ->
                composable(destination.route) { PlaceholderScreen(destination.label) }
            }
            composable(
                route = LIST_DETAIL_ROUTE,
                arguments = listOf(navArgument("listId") { type = NavType.LongType }),
                // Drill-down (list → its detail) is hierarchical, unlike the
                // bottom-tab switches above, so it gets the standard Android
                // push: new screen slides in from the right, pop reverses it.
                enterTransition = { slideInHorizontally(tween(300)) { it } },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } },
                popEnterTransition = { slideInHorizontally(tween(300)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } },
            ) { entry ->
                val listId = entry.arguments?.getLong("listId") ?: return@composable
                TaskListDetailScreen(listId = listId, onBack = { navController.popBackStack() })
            }
        }

        if (current != null) {
            FloatingNavBar(
                current = current,
                onSelect = { destination ->
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
