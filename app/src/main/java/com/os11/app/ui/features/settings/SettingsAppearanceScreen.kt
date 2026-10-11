package com.os11.app.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSegmented
import com.os11.app.ui.components.OSSwitch
import com.os11.app.ui.theme.OS11Density
import com.os11.app.ui.theme.OS11FontStep
import com.os11.app.ui.theme.OS11Theme

@Composable
@Suppress("TooGenericExceptionCaught")
fun SettingsAppearanceScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val accentPresets = listOf(
        Pair("#4da3ff", "Blue"),
        Pair("#10b981", "Emerald"),
        Pair("#8b5cf6", "Purple"),
        Pair("#f59e0b", "Amber"),
        Pair("#f43f5e", "Rose"),
        Pair("#06b6d4", "Cyan"),
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
                    text = "Appearance",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }
        }

        // 1. Accent Color
        item(key = "accent_color_section") {
            Text(
                text = "Accent Color",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                accentPresets.forEach { (hex, name) ->
                    val isSelected = state.appearance.accentColorHex.equals(hex, ignoreCase = true)
                    val swatchColor = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (_: Exception) {
                        colors.accent
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) colors.textPrimary else colors.borderGlass,
                                shape = CircleShape,
                            )
                            .clickable(
                                role = Role.RadioButton,
                                onClickLabel = "Select $name accent",
                            ) {
                                viewModel.setAccentColor(hex)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        // 2. Font Size
        item(key = "font_size_section") {
            Text(
                text = "Font Size",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            OSSegmented(
                options = listOf(OS11FontStep.Sm, OS11FontStep.Md, OS11FontStep.Lg, OS11FontStep.Xl),
                selectedOption = state.appearance.fontStep,
                onOptionSelected = { step ->
                    viewModel.setFontSize(step.name.lowercase())
                },
                labelProvider = { step ->
                    when (step) {
                        OS11FontStep.Sm -> "Small"
                        OS11FontStep.Md -> "Medium"
                        OS11FontStep.Lg -> "Large"
                        OS11FontStep.Xl -> "Extra"
                    }
                },
            )
        }

        // 3. Density
        item(key = "density_section") {
            Text(
                text = "Interface Density",
                style = type.sectionHeader.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s2))
            OSSegmented(
                options = listOf(OS11Density.Compact, OS11Density.Comfortable, OS11Density.Cozy),
                selectedOption = state.appearance.density,
                onOptionSelected = { density ->
                    viewModel.setDensity(density.name.lowercase())
                },
                labelProvider = { density ->
                    when (density) {
                        OS11Density.Compact -> "Compact"
                        OS11Density.Comfortable -> "Comfortable"
                        OS11Density.Cozy -> "Cozy"
                    }
                },
            )
        }

        // 4. Reduced Motion & Haptics
        item(key = "toggles_section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s4),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reduce Motion",
                            style = type.body.copy(color = colors.textPrimary),
                        )
                        Text(
                            text = "Minimize animation effects and transitions",
                            style = type.meta.copy(color = colors.textSecondary),
                        )
                    }
                    OSSwitch(
                        checked = state.appearance.reduceMotion,
                        onCheckedChange = { viewModel.setReduceMotion(it) },
                        contentDescription = "Reduce Motion",
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Feedback",
                            style = type.body.copy(color = colors.textPrimary),
                        )
                        Text(
                            text = "Tactile vibration for completion, drag, and errors",
                            style = type.meta.copy(color = colors.textSecondary),
                        )
                    }
                    OSSwitch(
                        checked = state.appearance.hapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) },
                        contentDescription = "Haptic Feedback",
                    )
                }
            }
        }
    }
}
