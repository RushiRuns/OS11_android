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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.theme.OS11Theme

@Composable
fun SettingsAboutScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
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
                    text = "About OS11",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }
        }

        // 1. App Header Card
        item(key = "app_header_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s6),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "OS11 Android",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
                Text(
                    text = "Version 1.0.0 (Build 2026.10)",
                    style = type.meta.copy(color = colors.textSecondary),
                )
                Text(
                    text = "Local-First Productivity System",
                    style = type.subViewTitle.copy(color = colors.accent),
                )
            }
        }

        // 2. Architectural Pillars
        item(key = "architecture_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Text(
                    text = "Architectural Philosophy",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )

                AboutPillarRow(
                    title = "Offline & Local First",
                    description = "SQLite WAL with zero cloud roundtrips for all CRUD operations",
                )
                AboutPillarRow(
                    title = "Deterministic Sync",
                    description = "Hybrid Logical Clocks (HLC) and outbox-backed tracked writes",
                )
                AboutPillarRow(
                    title = "Zero Battery Drain",
                    description = "Timestamp-derived state, exact alarms only for immediate next alert",
                )
                AboutPillarRow(
                    title = "Strict Privacy",
                    description = "Biometric app lock, FLAG_SECURE screen protection, no trackers",
                )
            }
        }
    }
}

@Composable
private fun AboutPillarRow(title: String, description: String) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = colors.success,
            modifier = Modifier.size(18.dp),
        )
        Column {
            Text(
                text = title,
                style = type.body.copy(color = colors.textPrimary),
            )
            Text(
                text = description,
                style = type.meta.copy(color = colors.textSecondary),
            )
        }
    }
}
