package com.os11.app.ui.features.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.theme.OS11Theme

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(OS11Theme.spacing.s4),
        contentAlignment = Alignment.Center,
    ) {
        OSEmptyState(
            title = "Settings",
            description = "App preferences, font scaling, density, and local backup configurations.",
        )
    }
}
