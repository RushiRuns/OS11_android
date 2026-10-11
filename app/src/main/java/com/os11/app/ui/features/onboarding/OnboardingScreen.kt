package com.os11.app.ui.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.service.settings.ProfilePreset
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.theme.OS11Theme
import com.os11.app.ui.util.rememberReducedMotion

private val EMOJI_OPTIONS = listOf(
    "📱", "💻", "🚀", "⚡", "🎯", "💡", "🌟", "🌿", "🛠️", "☕", "🧠", "🧭",
)

/**
 * Full-screen 3-step onboarding flow per ANDROID_VS_DESKTOP.md:
 * 1. Profile preset selection
 * 2. Display Name and Emoji setup
 * 3. Quick-add demo and offline-first storage explanation
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isReducedMotion = rememberReducedMotion()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing

    BackHandler(enabled = state.currentStep > 0) {
        viewModel.previousStep()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
    ) {
        // Step Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (state.currentStep > 0) {
                OSIconButton(
                    onClick = { viewModel.previousStep() },
                    contentDescription = "Previous step",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.textPrimary,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            StepProgressIndicator(
                currentStep = state.currentStep,
                totalSteps = OnboardingUiState.TOTAL_STEPS,
            )

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(spacing.s2))

        // Step Content Transition
        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                val duration = if (isReducedMotion) 0 else 250
                fadeIn(animationSpec = tween(duration)) togetherWith fadeOut(animationSpec = tween(duration))
            },
            label = "onboarding_step_transition",
            modifier = Modifier.weight(1f),
        ) { step ->
            when (step) {
                0 -> PresetStepContent(
                    selectedPreset = state.selectedPreset,
                    onSelectPreset = { viewModel.selectPreset(it) },
                    onContinue = { viewModel.nextStep() },
                )
                1 -> IdentityStepContent(
                    displayName = state.displayName,
                    onNameChange = { viewModel.updateDisplayName(it) },
                    selectedEmoji = state.selectedEmoji,
                    onSelectEmoji = { viewModel.selectEmoji(it) },
                    onContinue = { viewModel.nextStep() },
                )
                else -> QuickAddStepContent(
                    quickAddText = state.quickAddText,
                    onTextChange = { viewModel.updateQuickAddText(it) },
                    parsedResult = state.parsedResult,
                    isSubmitting = state.isSubmitting,
                    onFinishWithTask = {
                        viewModel.completeOnboarding(createStarterTask = true, onFinished = onFinished)
                    },
                    onSkipTask = {
                        viewModel.completeOnboarding(createStarterTask = false, onFinished = onFinished)
                    },
                )
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until totalSteps) {
            val isActive = i <= currentStep
            Box(
                modifier = Modifier
                    .size(width = if (i == currentStep) 24.dp else 12.dp, height = 4.dp)
                    .clip(RoundedCornerShape(radius.full))
                    .background(if (isActive) colors.accent else colors.borderSubtle),
            )
        }
    }
}

@Composable
private fun PresetStepContent(
    selectedPreset: ProfilePreset,
    onSelectPreset: (ProfilePreset) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Column(modifier = Modifier.padding(top = spacing.s2)) {
            Text(
                text = "Choose your workflow",
                style = type.viewTitle.copy(color = colors.textPrimary),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(spacing.s1))
            Text(
                text = "Start with a curated preset. You can toggle individual modules in Settings anytime.",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        ProfilePreset.AllPresets.forEach { preset ->
            PresetCard(
                preset = preset,
                isSelected = preset.id == selectedPreset.id,
                onClick = { onSelectPreset(preset) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OSButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(spacing.s4))
    }
}

@Composable
private fun PresetCard(preset: ProfilePreset, isSelected: Boolean, onClick: () -> Unit) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    val borderColor = if (isSelected) colors.accent else colors.borderSubtle
    val bgColor = if (isSelected) colors.surfaceRaised else colors.surfaceSidebar

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.lg))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(radius.lg))
            .clickable(role = Role.RadioButton) { onClick() }
            .padding(spacing.s4),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = preset.title,
                    style = type.subViewTitle.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.textOnAccent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Text(
                text = preset.description,
                style = type.meta.copy(color = colors.textSecondary),
            )
        }
    }
}

@Composable
private fun IdentityStepContent(
    displayName: String,
    onNameChange: (String) -> Unit,
    selectedEmoji: String,
    onSelectEmoji: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Column(modifier = Modifier.padding(top = spacing.s2)) {
            Text(
                text = "Personalize your device",
                style = type.viewTitle.copy(color = colors.textPrimary),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(spacing.s1))
            Text(
                text = "Identifies your local device in backups, exports, and future sync pairings.",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        // Selected avatar preview
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceRaised)
                    .border(2.dp, colors.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = selectedEmoji,
                    fontSize = 40.sp,
                )
            }
        }

        // Display Name input
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
            Text(
                text = "Device Display Name",
                style = type.meta.copy(color = colors.textPrimary, fontWeight = FontWeight.Medium),
            )
            OSTextField(
                value = displayName,
                onValueChange = onNameChange,
                placeholder = "e.g. My Phone or Alex's Device",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Emoji picker
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            Text(
                text = "Pick an avatar symbol",
                style = type.meta.copy(color = colors.textPrimary, fontWeight = FontWeight.Medium),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                EMOJI_OPTIONS.take(6).forEach { emoji ->
                    EmojiChip(
                        emoji = emoji,
                        isSelected = emoji == selectedEmoji,
                        onClick = { onSelectEmoji(emoji) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                EMOJI_OPTIONS.drop(6).take(6).forEach { emoji ->
                    EmojiChip(
                        emoji = emoji,
                        isSelected = emoji == selectedEmoji,
                        onClick = { onSelectEmoji(emoji) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OSButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(spacing.s4))
    }
}

@Composable
private fun EmojiChip(emoji: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = OS11Theme.colors
    val radius = OS11Theme.radius

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(radius.md))
            .background(if (isSelected) colors.surfaceRaised else colors.surfaceSidebar)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.accent else colors.borderSubtle,
                shape = RoundedCornerShape(radius.md),
            )
            .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 24.sp)
    }
}

@Composable
private fun QuickAddStepContent(
    quickAddText: String,
    onTextChange: (String) -> Unit,
    parsedResult: com.os11.domain.nlp.NlpParseResult,
    isSubmitting: Boolean,
    onFinishWithTask: () -> Unit,
    onSkipTask: () -> Unit,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val type = OS11Theme.type

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Column(modifier = Modifier.padding(top = spacing.s2)) {
            Text(
                text = "Natural capture & offline storage",
                style = type.viewTitle.copy(color = colors.textPrimary),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(spacing.s1))
            Text(
                text = "Fast capture from anywhere with natural dates, tags, and priorities.",
                style = type.meta.copy(color = colors.textSecondary),
            )
        }

        // Offline storage badge card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.lg))
                .background(colors.surfaceSidebar)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(radius.lg))
                .padding(spacing.s3),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        text = "100% Offline-First SQLite",
                        style = type.body.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(spacing.s1))
                    Text(
                        text = "Everything is stored in a private database on this phone. " +
                            "No cloud account required to use the app.",
                        style = type.meta.copy(color = colors.textSecondary),
                    )
                }
            }
        }

        // Interactive Quick Add Demo Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.lg))
                .background(colors.surfaceRaised)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(radius.lg))
                .padding(spacing.s3),
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Text(
                text = "Try Quick Add",
                style = type.meta.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold),
            )

            OSTextField(
                value = quickAddText,
                onValueChange = onTextChange,
                placeholder = "Type task with dates, #tags, or !priority...",
                modifier = Modifier.fillMaxWidth(),
            )

            // Real-time NLP parsed tokens preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!parsedResult.dueDate.isNullOrBlank()) {
                    OSChip(
                        label = parsedResult.dueDate.orEmpty(),
                        selected = true,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                }
                if (parsedResult.priority > 0) {
                    val priorityLabel = when (parsedResult.priority) {
                        1 -> "Low"
                        2 -> "Medium"
                        3 -> "High"
                        else -> "Critical"
                    }
                    OSChip(label = "!$priorityLabel", selected = true)
                }
                parsedResult.tags.take(2).forEach { tag ->
                    OSChip(label = "#$tag", selected = true)
                }
            }

            Text(
                text = "Syntax: @Area  /Project  #tag  !low|medium|high|critical  tomorrow 10am",
                style = type.meta.copy(color = colors.textSecondary),
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            OSButton(
                onClick = onFinishWithTask,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSubmitting) "Setting up..." else "Create First Task & Finish")
            }

            OSButton(
                onClick = onSkipTask,
                variant = OSButtonVariant.Secondary,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Skip task & finish setup")
            }
        }

        Spacer(modifier = Modifier.height(spacing.s4))
    }
}
