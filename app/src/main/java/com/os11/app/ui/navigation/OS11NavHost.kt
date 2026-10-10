package com.os11.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.os11.app.ui.features.area.AreaOverviewScreen
import com.os11.app.ui.features.browse.BrowseScreen
import com.os11.app.ui.features.dashboard.DashboardScreen
import com.os11.app.ui.features.gallery.ComponentGalleryScreen
import com.os11.app.ui.features.goals.GoalsScreen
import com.os11.app.ui.features.inbox.InboxScreen
import com.os11.app.ui.features.myday.MyDayScreen
import com.os11.app.ui.features.planned.PlannedScreen
import com.os11.app.ui.features.pomodoro.PomodoroScreen
import com.os11.app.ui.features.project.ProjectScreen
import com.os11.app.ui.features.settings.SettingsScreen
import com.os11.app.ui.features.smartlists.AllTasksScreen
import com.os11.app.ui.features.smartlists.CompletedScreen
import com.os11.app.ui.features.smartlists.ImportantScreen
import com.os11.app.ui.features.trash.TrashScreen

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
            BrowseScreen(
                onNavigate = { route -> navController.navigate(route) },
            )
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
            SettingsScreen(
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable<OS11Route.Gallery> {
            ComponentGalleryScreen()
        }
        composable<OS11Route.Trash> {
            TrashScreen()
        }
        composable<OS11Route.AreaOverview> {
            AreaOverviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectClick = { projectId -> navController.navigate(OS11Route.ProjectDetail(projectId)) },
            )
        }
        composable<OS11Route.ProjectDetail> {
            ProjectScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<OS11Route.Important> {
            ImportantScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<OS11Route.AllTasks> {
            AllTasksScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<OS11Route.Completed> {
            CompletedScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<OS11Route.Search> {
            com.os11.app.ui.features.search.SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable<OS11Route.TagDetail> {
            com.os11.app.ui.features.tags.TagDetailScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<OS11Route.TagManager> {
            com.os11.app.ui.features.tags.TagManagerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<OS11Route.NotificationCenter> {
            com.os11.app.ui.features.notification.NotificationCenterScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
