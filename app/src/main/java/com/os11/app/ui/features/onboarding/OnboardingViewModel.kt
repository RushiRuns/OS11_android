package com.os11.app.ui.features.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.settings.ProfilePreset
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.domain.id.DeterministicIds
import com.os11.domain.nlp.NlpParseResult
import com.os11.domain.nlp.NlpParser
import com.os11.domain.result.onSuccess
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val selectedPreset: ProfilePreset = ProfilePreset.Standard,
    val displayName: String = "My Android",
    val selectedEmoji: String = "📱",
    val quickAddText: String = DEFAULT_SAMPLE_TASK,
    val parsedResult: NlpParseResult = NlpParseResult(title = ""),
    val isCompleted: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    companion object {
        const val DEFAULT_SAMPLE_TASK = "Review quarterly goals tomorrow 10am #focus !high"
        const val TOTAL_STEPS = 3
    }
}

/**
 * ViewModel managing the 3-step onboarding flow per ANDROID_VS_DESKTOP.md:
 * 1. Profile preset selection (Minimal, Standard, Full GTD, Power)
 * 2. Display Name and Emoji setup
 * 3. Quick-add demo and offline-first storage explanation
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsService: SettingsService,
    private val taskService: TaskService,
    private val tagService: TagService,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            currentStep = savedStateHandle[KEY_STEP] ?: 0,
            parsedResult = parseNlp(OnboardingUiState.DEFAULT_SAMPLE_TASK),
        ),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val identity = settingsService.getLocalIdentity()
            if (identity != null) {
                _uiState.update { current ->
                    if (current.displayName == "My Android" || current.displayName.isBlank()) {
                        current.copy(
                            displayName = identity.displayName,
                            selectedEmoji = identity.avatarEmoji ?: current.selectedEmoji,
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun selectPreset(preset: ProfilePreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
        viewModelScope.launch {
            settingsService.applyProfilePreset(preset.id)
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun selectEmoji(emoji: String) {
        _uiState.update { it.copy(selectedEmoji = emoji) }
    }

    fun updateQuickAddText(text: String) {
        val parsed = parseNlp(text)
        _uiState.update {
            it.copy(
                quickAddText = text,
                parsedResult = parsed,
            )
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current == 0) {
            setStep(1)
        } else if (current == 1) {
            val state = _uiState.value
            setStep(2)
            viewModelScope.launch {
                settingsService.updateLocalIdentity(state.displayName, state.selectedEmoji)
            }
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 0) {
            setStep(current - 1)
        }
    }

    fun completeOnboarding(createStarterTask: Boolean = true, onFinished: () -> Unit = {}) {
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val state = _uiState.value
                settingsService.updateLocalIdentity(state.displayName, state.selectedEmoji)

                if (createStarterTask && state.quickAddText.isNotBlank()) {
                    createSampleTask(state.parsedResult, state.quickAddText)
                }

                settingsService.setOnboardingCompleted(true)
                _uiState.update { it.copy(isCompleted = true) }
                onFinished()
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun setStep(step: Int) {
        savedStateHandle[KEY_STEP] = step
        _uiState.update { it.copy(currentStep = step) }
    }

    private fun parseNlp(text: String): NlpParseResult {
        if (text.isBlank()) return NlpParseResult(title = "")
        val refDate = try {
            LocalDate.parse(clock.today())
        } catch (_: Exception) {
            LocalDate.now()
        }
        return NlpParser.parse(text, refDate)
    }

    private suspend fun createSampleTask(parsed: NlpParseResult, rawInput: String) {
        val workspaceId = DeterministicIds.defaultWorkspaceId()
        val finalTitle = parsed.title.trim().ifEmpty { rawInput.trim() }

        val result = taskService.createTask(
            title = finalTitle,
            workspaceId = workspaceId,
            dueDate = parsed.dueDate,
            dueTime = parsed.dueTime,
            priority = parsed.priority,
        )

        result.onSuccess { task ->
            parsed.tags.forEach { tagName ->
                tagService.getOrCreateTag(tagName).onSuccess { tagItem ->
                    tagService.addTagToTask(task.id, tagItem.id)
                }
            }
            if (parsed.dueDate == clock.today()) {
                taskService.addToMyDay(task.id, clock.today())
            }
        }
    }

    companion object {
        private const val KEY_STEP = "onboarding_current_step"
    }
}
