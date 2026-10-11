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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
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
import com.os11.app.service.settings.ModuleItem
import com.os11.app.service.settings.ProfilePreset
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSwitch
import com.os11.app.ui.theme.OS11Theme

@Composable
fun SettingsModulesScreen(
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
                    text = "Modules & Presets",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }
        }

        // 1. Profile Presets
        item(key = "profile_presets_section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(
                    text = "Profile Presets",
                    style = type.subViewTitle.copy(color = colors.textPrimary),
                )
                Text(
                    text = "Quickly configure recommended module sets based on your workflow",
                    style = type.meta.copy(color = colors.textSecondary),
                )
                Spacer(modifier = Modifier.height(spacing.s2))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    ProfilePreset.AllPresets.forEach { preset ->
                        OSChip(
                            label = preset.title,
                            selected = state.activePreset == preset.id,
                            onClick = { viewModel.applyProfilePreset(preset) },
                        )
                    }
                }
            }
        }

        // 2. Module Toggles Header
        item(key = "module_toggles_header") {
            Text(
                text = "Module Toggles",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
        }

        // 3. Module Toggles List
        items(state.modules, key = { it.name }) { module ->
            ModuleToggleRow(
                module = module,
                onCheckedChange = { enabled ->
                    viewModel.setModuleEnabled(module.name, enabled)
                },
            )
        }

        // 4. Local Info Card
        item(key = "info_card") {
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
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Modules are device-local: disabling a module removes it from Browse, " +
                        "search actions and shortcuts without deleting your data.",
                    style = type.meta.copy(color = colors.textSecondary),
                )
            }
            Spacer(modifier = Modifier.height(spacing.s6))
        }
    }
}

@Composable
private fun ModuleToggleRow(module: ModuleItem, onCheckedChange: (Boolean) -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.md))
            .padding(spacing.s4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = module.title,
                style = type.body.copy(color = colors.textPrimary),
            )
            Text(
                text = module.description,
                style = type.meta.copy(color = colors.textSecondary),
            )
        }
        OSSwitch(
            checked = module.isEnabled,
            onCheckedChange = onCheckedChange,
            contentDescription = module.title,
        )
    }
}
