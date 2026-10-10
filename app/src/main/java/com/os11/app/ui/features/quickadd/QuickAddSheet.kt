package com.os11.app.ui.features.quickadd

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSChip
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSheet
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.components.ParsePreviewChips
import com.os11.app.ui.theme.OS11Theme

/**
 * Modal Quick Add sheet with live NLP parsing, @/#// autocompletion, voice input,
 * and continuous multi-task rapid entry (stays open after Enter/Add).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    defaultAreaId: String? = null,
    defaultProjectId: String? = null,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onVoiceResult(spoken)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OSSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s3),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Quick Add",
                    style = type.sectionHeader.copy(color = colors.textPrimary),
                )
                OSIconButton(
                    onClick = onDismissRequest,
                    contentDescription = "Close Quick Add",
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
            }

            // Input Field with Voice Button
            OSTextField(
                value = state.text,
                onValueChange = { viewModel.onTextChanged(it) },
                placeholder = "Task title, @area, /project, #tag, dates...",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        viewModel.submitTask(defaultAreaId, defaultProjectId)
                    },
                ),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
                    ) {
                        OSIconButton(
                            onClick = {
                                launchVoiceInput(speechLauncher)
                            },
                            contentDescription = "Voice input",
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = colors.accent,
                            )
                        }
                    }
                },
            )

            // Autocomplete suggestions
            if (state.suggestions.isNotEmpty()) {
                AutocompleteRow(
                    suggestions = state.suggestions,
                    onSelect = { viewModel.applySuggestion(it) },
                )
            }

            // NLP Parse Preview Chips
            ParsePreviewChips(
                parseResult = state.parseResult,
                onRemoveToken = { viewModel.removeToken(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            // Feedback & Action Row
            ActionFooter(
                lastAddedTitle = state.lastAddedTitle,
                isSubmitting = state.isSubmitting,
                canSubmit = state.text.isNotBlank(),
                onSubmit = {
                    viewModel.submitTask(defaultAreaId, defaultProjectId)
                },
                onDone = {
                    keyboardController?.hide()
                    onDismissRequest()
                },
            )
        }
    }
}

private fun launchVoiceInput(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak task details...")
        }
        launcher.launch(intent)
    } catch (_: ActivityNotFoundException) {
        // Fallback gracefully on devices without speech recognizer
    }
}

@Composable
private fun AutocompleteRow(
    suggestions: List<AutocompleteSuggestion>,
    onSelect: (AutocompleteSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = OS11Theme.spacing
    val colors = OS11Theme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        suggestions.forEach { suggestion ->
            val tint = when (suggestion.trigger) {
                '@' -> colors.accent
                '/' -> colors.tags.blue
                '#' -> colors.tags.purple
                else -> colors.textSecondary
            }
            OSChip(
                label = "${suggestion.trigger}${suggestion.name}",
                tintColor = tint,
                onClick = { onSelect(suggestion) },
            )
        }
    }
}

@Composable
private fun ActionFooter(
    lastAddedTitle: String?,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Confirmation feedback
        Box(modifier = Modifier.weight(1f)) {
            if (lastAddedTitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s1),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Added \"$lastAddedTitle\"",
                        style = type.meta.copy(color = colors.textSecondary),
                        maxLines = 1,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OSButton(
                onClick = onDone,
                variant = com.os11.app.ui.components.OSButtonVariant.Secondary,
                modifier = Modifier.height(36.dp),
            ) {
                Text(text = "Done", style = type.body)
            }
            OSButton(
                onClick = onSubmit,
                enabled = canSubmit && !isSubmitting,
                modifier = Modifier.height(36.dp),
            ) {
                Text(text = "Add", style = type.body)
            }
        }
    }
}
