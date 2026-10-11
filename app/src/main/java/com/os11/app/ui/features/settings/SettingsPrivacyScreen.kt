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
import androidx.compose.material.icons.outlined.Lock
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
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSwitch
import com.os11.app.ui.theme.OS11Theme

@Composable
fun SettingsPrivacyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val timeoutOptions = listOf(
        Pair(0, "Immediately"),
        Pair(60, "1 Minute"),
        Pair(300, "5 Minutes"),
        Pair(900, "15 Minutes"),
    )

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
                    text = "Privacy & Security",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }
        }

        // 1. Biometric App Lock Card
        item(key = "app_lock_card") {
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp),
                        )
                        Column {
                            Text(
                                text = "Biometric App Lock",
                                style = type.body.copy(color = colors.textPrimary),
                            )
                            Text(
                                text = "Require fingerprint, face, or PIN fallback",
                                style = type.meta.copy(color = colors.textSecondary),
                            )
                        }
                    }
                    OSSwitch(
                        checked = state.security.appLockEnabled,
                        onCheckedChange = { viewModel.setAppLockEnabled(it) },
                        contentDescription = "Biometric App Lock",
                    )
                }

                if (state.security.appLockEnabled) {
                    Spacer(modifier = Modifier.height(spacing.s1))
                    Text(
                        text = "Lock Timeout After Backgrounding",
                        style = type.meta.copy(color = colors.textPrimary),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    ) {
                        timeoutOptions.forEach { (seconds, label) ->
                            OSChip(
                                label = label,
                                selected = state.security.appLockTimeoutSeconds == seconds,
                                onClick = { viewModel.setAppLockTimeoutSeconds(seconds) },
                            )
                        }
                    }
                }
            }
        }

        // 2. Screen Protection (FLAG_SECURE)
        item(key = "secure_screen_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp),
                        )
                        Column {
                            Text(
                                text = "Secure Screen",
                                style = type.body.copy(color = colors.textPrimary),
                            )
                            Text(
                                text = "Block screenshots and hide app preview in recents task switcher",
                                style = type.meta.copy(color = colors.textSecondary),
                            )
                        }
                    }
                    OSSwitch(
                        checked = state.security.secureScreen,
                        onCheckedChange = { viewModel.setSecureScreen(it) },
                        contentDescription = "Secure Screen",
                    )
                }
            }
        }

        // 3. Privacy Guarantee Card
        item(key = "privacy_statement_card") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.md))
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.md))
                    .padding(spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Zero Cloud Telemetry: OS11 has no analytics SDKs, no cloud tracking, and no external " +
                        "telemetry. All databases reside exclusively in app-private storage.",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
        }
    }
}
