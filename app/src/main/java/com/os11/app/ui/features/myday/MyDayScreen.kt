package com.os11.app.ui.features.myday

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.theme.OS11Theme

@Composable
fun MyDayScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(OS11Theme.spacing.s4),
        contentAlignment = Alignment.Center,
    ) {
        OSEmptyState(
            title = "Focus on today",
            description = "Tasks you plan for today will appear here. Tap + to add a task.",
        )
    }
}
