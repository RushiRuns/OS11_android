package com.os11.app.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.theme.OS11Theme

@Composable
fun SettingsDataScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(horizontal = spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        item(key = "top_bar") {
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSIconButton(
                    onClick = onNavigateBack,
                    contentDescription = "Navigate back",
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
                Text(
                    text = "Data & Storage",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }
        }

        // 1. Local Database Overview
        item(key = "database_overview_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Local Storage (SQLite WAL)",
                        style = type.subViewTitle.copy(color = colors.textPrimary),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DataStatPill("Tasks", state.dataCounts.taskCount.toString())
                    DataStatPill("Projects", state.dataCounts.projectCount.toString())
                    DataStatPill("Areas", state.dataCounts.areaCount.toString())
                    DataStatPill("Tags", state.dataCounts.tagCount.toString())
                }
            }
        }

        // 2. Synced Settings Allowlist
        item(key = "sync_allowlist_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Synced Settings Allowlist",
                        style = type.subViewTitle.copy(color = colors.textPrimary),
                    )
                }

                Text(
                    text = "Per SCHEMA.md, exactly 7 settings keys travel across devices during sync. " +
                        "All other preferences are strictly device-local.",
                    style = type.meta.copy(color = colors.textSecondary),
                )

                Spacer(modifier = Modifier.height(spacing.s1))

                TrackedWriter.SYNCED_SETTINGS_ALLOWLIST.forEach { key ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Synced",
                            tint = colors.success,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = key,
                            style = type.mono.copy(color = colors.textPrimary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataStatPill(label: String, value: String) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceGlass)
            .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.md))
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = type.subViewTitle.copy(color = colors.textPrimary),
        )
        Text(
            text = label,
            style = type.meta.copy(color = colors.textSecondary),
        )
    }
}
