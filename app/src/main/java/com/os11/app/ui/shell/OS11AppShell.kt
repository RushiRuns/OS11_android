package com.os11.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.os11.app.service.undo.UndoManager
import com.os11.app.ui.components.OSGlassMenu
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSMenuItem
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSSnackbarHost
import com.os11.app.ui.navigation.OS11NavHost
import com.os11.app.ui.navigation.OS11Route
import com.os11.app.ui.theme.OS11Theme
import com.os11.app.ui.theme.os11MainPaneBackground

enum class OS11Tab(val route: OS11Route, val title: String, val icon: ImageVector) {
    MyDay(OS11Route.MyDay, "My Day", Icons.Default.Home),
    Inbox(OS11Route.Inbox, "Inbox", Icons.Default.MailOutline),
    Planned(OS11Route.Planned, "Planned", Icons.Default.DateRange),
    Browse(OS11Route.Browse, "Browse", Icons.AutoMirrored.Filled.List),
}

/**
 * OS11 Main Application Shell.
 *
 * Implements:
 * 1. Edge-to-edge layout with single-point window insets handling
 * 2. Top Bar (height 52dp) with active destination and Profile Avatar menu
 * 3. 4-Tab Bottom Bar (height 64dp) with 48dp minimum touch targets
 * 4. 56dp Quick Add Floating Action Button
 * 5. Glass-style profile menu sheet per ADR-A07
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OS11AppShell(modifier: Modifier = Modifier, undoManager: UndoManager? = null) {
    val navController = rememberNavController()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val layout = OS11Theme.layout
    val type = OS11Theme.type

    var showProfileMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (undoManager != null) {
        LaunchedEffect(undoManager) {
            undoManager.undoEvents.collect { token ->
                val result = snackbarHostState.showSnackbar(
                    message = token.actionDescription,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    undoManager.undo(token.id)
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screenTitle = resolveScreenTitle(currentRoute)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(layout.topBarHeight)
                .background(colors.surfaceSidebar)
                .padding(horizontal = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = screenTitle,
                style = type.subViewTitle.copy(color = colors.textPrimary),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                // Profile Avatar Button
                OSIconButton(
                    onClick = { showProfileMenu = true },
                    contentDescription = "Profile Menu",
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(spacing.px, colors.borderAvatar, CircleShape)
                            .background(colors.surfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = colors.textSecondary,
                        )
                    }
                }
            }
        }

        // 2. Main Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .os11MainPaneBackground(colors),
        ) {
            OS11NavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
            )

            // FAB for Quick Add
            FloatingActionButton(
                onClick = { /* Quick Add Sheet */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.s4)
                    .size(layout.fabSize),
                containerColor = colors.accent,
                contentColor = colors.textOnAccent,
                shape = RoundedCornerShape(OS11Theme.radius.xl),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add",
                )
            }

            // Snackbar host for Undo actions
            OSSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = spacing.s4),
            )
        }

        // 3. Bottom Bar
        OS11BottomNavBar(
            currentRoute = currentRoute,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }

    // Profile Menu Modal Sheet
    if (showProfileMenu) {
        OSSheet(onDismissRequest = { showProfileMenu = false }) {
            Box(modifier = Modifier.padding(spacing.s4)) {
                OSGlassMenu(
                    items = listOf(
                        OSMenuItem(
                            title = "Dashboard",
                            onClick = {
                                showProfileMenu = false
                                navController.navigate(OS11Route.Dashboard)
                            },
                        ),
                        OSMenuItem(
                            title = "Goals",
                            onClick = {
                                showProfileMenu = false
                                navController.navigate(OS11Route.Goals)
                            },
                        ),
                        OSMenuItem(
                            title = "Pomodoro",
                            onClick = {
                                showProfileMenu = false
                                navController.navigate(OS11Route.Pomodoro)
                            },
                        ),
                        OSMenuItem(
                            title = "Trash",
                            onClick = {
                                showProfileMenu = false
                                navController.navigate(OS11Route.Trash)
                            },
                        ),
                        OSMenuItem(
                            title = "Settings",
                            onClick = {
                                showProfileMenu = false
                                navController.navigate(OS11Route.Settings)
                            },
                        ),
                        OSMenuItem(
                            title = "Component Gallery (Debug)",
                            onClick = {
                                showProfileMenu = false
                                navController.navigate(OS11Route.Gallery)
                            },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun OS11BottomNavBar(currentRoute: String?, onNavigate: (OS11Route) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val layout = OS11Theme.layout
    val type = OS11Theme.type

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(layout.bottomBarHeight)
            .background(colors.surfaceSidebar)
            .border(spacing.px, colors.borderSubtle)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OS11Tab.entries.forEach { tab ->
            val isSelected = currentRoute?.contains(tab.name) == true
            val tabColor = if (isSelected) colors.accent else colors.textSecondary
            val interactionSource = remember { MutableInteractionSource() }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(layout.bottomBarHeight)
                    .clickable(
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = ripple(color = colors.surfaceHover),
                        onClick = { onNavigate(tab.route) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = tabColor,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.title,
                    style = type.meta.copy(color = tabColor),
                )
            }
        }
    }
}

private fun resolveScreenTitle(currentRoute: String?): String {
    if (currentRoute == null) return "OS11"
    return when {
        currentRoute.contains("MyDay") -> "My Day"
        currentRoute.contains("Inbox") -> "Inbox"
        currentRoute.contains("Planned") -> "Planned"
        currentRoute.contains("Browse") -> "Browse"
        currentRoute.contains("Dashboard") -> "Dashboard"
        currentRoute.contains("Goals") -> "Goals"
        currentRoute.contains("Pomodoro") -> "Pomodoro"
        currentRoute.contains("Settings") -> "Settings"
        currentRoute.contains("Gallery") -> "Gallery"
        currentRoute.contains("Trash") -> "Trash"
        else -> "OS11"
    }
}
