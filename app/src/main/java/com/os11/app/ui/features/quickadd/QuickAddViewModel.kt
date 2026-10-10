package com.os11.app.ui.features.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.area.AreaItem
import com.os11.app.service.area.AreaService
import com.os11.app.service.project.ProjectItem
import com.os11.app.service.project.ProjectService
import com.os11.app.service.tag.TagItem
import com.os11.app.service.tag.TagService
import com.os11.app.service.task.TaskService
import com.os11.domain.id.DeterministicIds
import com.os11.domain.nlp.NlpParseResult
import com.os11.domain.nlp.NlpParser
import com.os11.domain.nlp.NlpTokenMatch
import com.os11.domain.result.onSuccess
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AutocompleteSuggestion(val trigger: Char, val id: String, val name: String, val detail: String? = null)

data class QuickAddUiState(
    val text: String = "",
    val parseResult: NlpParseResult = NlpParseResult(title = ""),
    val suggestions: List<AutocompleteSuggestion> = emptyList(),
    val lastAddedTitle: String? = null,
    val isSubmitting: Boolean = false,
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
@Suppress("TooManyFunctions")
class QuickAddViewModel @Inject constructor(
    private val taskService: TaskService,
    private val areaService: AreaService,
    private val projectService: ProjectService,
    private val tagService: TagService,
    private val clock: Clock,
) : ViewModel() {

    private val workspaceId = DeterministicIds.defaultWorkspaceId()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val lastAddedTitleState = MutableStateFlow<String?>(null)
    private val isSubmittingState = MutableStateFlow(false)

    private val areasFlow = areaService.observeAreaItems(workspaceId)
    private val projectsFlow = projectService.observeProjectItems(workspaceId)
    private val tagsFlow = tagService.observeTagItems()

    private val parseResultFlow = _text
        .debounce(DEBOUNCE_MS)
        .distinctUntilChanged()
        .map { rawText ->
            if (rawText.isBlank()) {
                NlpParseResult(title = "")
            } else {
                val refDate = try {
                    LocalDate.parse(clock.today())
                } catch (_: Exception) {
                    LocalDate.now()
                }
                NlpParser.parse(rawText, refDate)
            }
        }
        .flowOn(Dispatchers.Default)

    private val suggestionsFlow = combine(
        _text,
        areasFlow,
        projectsFlow,
        tagsFlow,
    ) { currentText, areas, projects, tags ->
        resolveSuggestions(currentText, areas, projects, tags)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<QuickAddUiState> = combine(
        _text,
        parseResultFlow,
        suggestionsFlow,
        lastAddedTitleState,
        isSubmittingState,
    ) { currentText, parsed, suggestions, lastAdded, submitting ->
        QuickAddUiState(
            text = currentText,
            parseResult = parsed,
            suggestions = suggestions,
            lastAddedTitle = lastAdded,
            isSubmitting = submitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS),
        initialValue = QuickAddUiState(),
    )

    fun onTextChanged(newText: String) {
        _text.value = newText
        if (lastAddedTitleState.value != null) {
            lastAddedTitleState.value = null
        }
    }

    fun onVoiceResult(spokenText: String) {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) return
        val current = _text.value.trim()
        _text.value = if (current.isEmpty()) trimmed else "$current $trimmed"
    }

    fun removeToken(token: NlpTokenMatch) {
        val current = _text.value
        val updated = current.replace(token.rawText, " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        _text.value = updated
    }

    fun applySuggestion(suggestion: AutocompleteSuggestion) {
        val current = _text.value
        val lastWord = current.substringAfterLast(' ')
        val prefix = current.substringBeforeLast(' ', "")
        val replacement = "${suggestion.trigger}${suggestion.name} "
        val updated = if (prefix.isEmpty()) {
            replacement
        } else {
            "$prefix $replacement"
        }
        _text.value = updated
    }

    fun submitTask(defaultAreaId: String? = null, defaultProjectId: String? = null, onSuccess: (() -> Unit)? = null) {
        val currentInput = _text.value.trim()
        if (currentInput.isEmpty() || isSubmittingState.value) return

        val refDate = try {
            LocalDate.parse(clock.today())
        } catch (_: Exception) {
            LocalDate.now()
        }
        val parsed = NlpParser.parse(currentInput, refDate)
        val finalTitle = parsed.title.trim().ifEmpty { currentInput }

        viewModelScope.launch {
            isSubmittingState.value = true
            try {
                createAndConfigureTask(parsed, finalTitle, defaultAreaId, defaultProjectId)
                lastAddedTitleState.value = finalTitle
                _text.value = ""
                onSuccess?.invoke()
            } finally {
                isSubmittingState.value = false
            }
        }
    }

    private suspend fun createAndConfigureTask(
        parsed: NlpParseResult,
        title: String,
        defaultAreaId: String?,
        defaultProjectId: String?,
    ) {
        val resolvedAreaId = findAreaId(parsed.areaName) ?: defaultAreaId
        val resolvedProjectId = findProjectId(parsed.projectName) ?: defaultProjectId

        val result = taskService.createTask(
            title = title,
            workspaceId = workspaceId,
            areaId = resolvedAreaId,
            projectId = resolvedProjectId,
            dueDate = parsed.dueDate,
            dueTime = parsed.dueTime,
            priority = parsed.priority,
        )

        result.onSuccess { task ->
            if (parsed.tags.isNotEmpty()) {
                attachTags(task.id, parsed.tags)
            }
            if (parsed.dueDate == clock.today()) {
                taskService.addToMyDay(task.id, clock.today())
            }
        }
    }

    private suspend fun attachTags(taskId: String, tagNames: List<String>) {
        tagNames.forEach { name ->
            val tagResult = tagService.getOrCreateTag(name)
            tagResult.onSuccess { tagItem ->
                tagService.addTagToTask(taskId, tagItem.id)
            }
        }
    }

    private suspend fun findAreaId(areaName: String?): String? {
        if (areaName.isNullOrBlank()) return null
        return try {
            val areas = areasFlow.first()
            areas.firstOrNull { it.name.equals(areaName, ignoreCase = true) }?.id
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun findProjectId(projectName: String?): String? {
        if (projectName.isNullOrBlank()) return null
        return try {
            val projects = projectsFlow.first()
            projects.firstOrNull { it.name.equals(projectName, ignoreCase = true) }?.id
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveSuggestions(
        currentText: String,
        areas: List<AreaItem>,
        projects: List<ProjectItem>,
        tags: List<TagItem>,
    ): List<AutocompleteSuggestion> {
        val lastWord = currentText.substringAfterLast(' ')
        if (lastWord.length < MIN_SUGGESTION_PREFIX_LEN) return emptyList()

        return when (lastWord.first()) {
            '@' -> {
                val query = lastWord.drop(1).lowercase()
                areas.filter { it.name.lowercase().contains(query) }
                    .take(MAX_SUGGESTIONS)
                    .map { AutocompleteSuggestion('@', it.id, it.name, "Area") }
            }
            '/' -> {
                val query = lastWord.drop(1).lowercase()
                projects.filter { it.name.lowercase().contains(query) }
                    .take(MAX_SUGGESTIONS)
                    .map { AutocompleteSuggestion('/', it.id, it.name, "Project") }
            }
            '#' -> {
                val query = lastWord.drop(1).lowercase()
                tags.filter { it.name.lowercase().contains(query) }
                    .take(MAX_SUGGESTIONS)
                    .map { AutocompleteSuggestion('#', it.id, it.name, "Tag") }
            }
            else -> emptyList()
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 150L
        private const val SUBSCRIBE_TIMEOUT_MS = 5000L
        private const val MIN_SUGGESTION_PREFIX_LEN = 1
        private const val MAX_SUGGESTIONS = 6
    }
}
