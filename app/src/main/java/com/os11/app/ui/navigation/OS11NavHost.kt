package com.os11.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.os11.app.ui.features.browse.BrowseScreen
import com.os11.app.ui.features.dashboard.DashboardScreen
import com.os11.app.ui.features.gallery.ComponentGalleryScreen
import com.os11.app.ui.features.goals.GoalsScreen
import com.os11.app.ui.features.inbox.InboxScreen
import com.os11.app.ui.features.myday.MyDayScreen
import com.os11.app.ui.features.planned.PlannedScreen
import com.os11.app.ui.features.pomodoro.PomodoroScreen
import com.os11.app.ui.features.settings.SettingsScreen

/**
 * Main application navigation graph with type-safe route destinations.
 */
@Composable
fun OS11NavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: OS11Route = OS11Route.MyDay,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<OS11Route.MyDay> {
            MyDayScreen()
        }
        composable<OS11Route.Inbox> {
            InboxScreen()
        }
        composable<OS11Route.Planned> {
            PlannedScreen()
        }
        composable<OS11Route.Browse> {
            BrowseScreen()
        }
        composable<OS11Route.Dashboard> {
            DashboardScreen()
        }
        composable<OS11Route.Goals> {
            GoalsScreen()
        }
        composable<OS11Route.Pomodoro> {
            PomodoroScreen()
        }
        composable<OS11Route.Settings> {
            SettingsScreen()
        }
        composable<OS11Route.Gallery> {
            ComponentGalleryScreen()
        }
    }
}
