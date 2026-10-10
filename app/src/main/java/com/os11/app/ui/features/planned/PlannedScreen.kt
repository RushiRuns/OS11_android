package com.os11.app.ui.features.planned

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.os11.app.ui.components.OSEmptyState
import com.os11.app.ui.theme.OS11Theme

@Composable
fun PlannedScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(OS11Theme.spacing.s4),
        contentAlignment = Alignment.Center,
    ) {
        OSEmptyState(
            title = "Nothing scheduled",
            description = "Upcoming tasks with due dates or reminders will be organized here.",
        )
    }
}
