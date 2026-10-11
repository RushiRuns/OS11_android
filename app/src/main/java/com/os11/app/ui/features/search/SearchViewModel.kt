package com.os11.app.ui.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.search.SearchService
import com.os11.app.service.settings.SettingsService
import com.os11.app.service.task.TaskItem
import com.os11.app.service.task.TaskService
import com.os11.app.ui.navigation.OS11Route
import com.os11.domain.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchActionKind {
    Navigate,
    CreateTask,
}

data class SearchActionItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val kind: SearchActionKind,
    val targetRoute: OS11Route? = null,
)

data class SearchTaskSnippet(val task: TaskItem, val snippet: String? = null)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val actions: List<SearchActionItem> = emptyList(),
    val tasks: List<SearchTaskSnippet> = emptyList(),
    val recentSearches: List<String> = emptyList(),
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
@Suppress("TooManyFunctions")
class SearchViewModel @Inject constructor(
    private val searchService: SearchService,
    private val taskService: TaskService,
    private val settingsService: SettingsService,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val isSearchingState = MutableStateFlow(false)
    private val recentSearchesFlow = searchService.observeRecentSearches()
    private val taskResultsState = MutableStateFlow<List<SearchTaskSnippet>>(emptyList())
    private val enabledModulesFlow = settingsService.observeEnabledModuleNames()

    val uiState: StateFlow<SearchUiState> = combine(
        combine(_query, isSearchingState) { q, searching -> Pair(q, searching) },
        combine(taskResultsState, recentSearchesFlow, enabledModulesFlow) { tasks, recents, modules ->
            Triple(tasks, recents, modules)
        },
    ) { (currentQuery, searching), (tasks, recents, modules) ->
        SearchUiState(
            query = currentQuery,
            isSearching = searching,
            actions = buildActions(currentQuery, modules),
            tasks = tasks,
            recentSearches = recents,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS),
        initialValue = SearchUiState(),
    )

    init {
        viewModelScope.launch {
            _query
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { q ->
                    performSearch(q)
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    fun onRecentSelected(selectedQuery: String) {
        _query.value = selectedQuery
    }

    fun removeRecentSearch(queryToRemove: String) {
        viewModelScope.launch {
            searchService.removeRecentSearch(queryToRemove)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            searchService.clearRecentSearches()
        }
    }

    fun toggleTaskComplete(task: TaskItem) {
        viewModelScope.launch {
            taskService.setCompleted(task.id, !task.isCompleted)
            performSearch(_query.value)
        }
    }

    private suspend fun performSearch(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) {
            taskResultsState.value = emptyList()
            isSearchingState.value = false
            return
        }

        isSearchingState.value = true
        try {
            val result = searchService.searchTaskItems(trimmed, limit = MAX_SEARCH_RESULTS)
            result.onSuccess { list ->
                taskResultsState.value = list.map { task ->
                    SearchTaskSnippet(
                        task = task,
                        snippet = extractSnippet(task, trimmed),
                    )
                }
                searchService.addRecentSearch(trimmed)
            }
        } finally {
            isSearchingState.value = false
        }
    }

    private fun extractSnippet(task: TaskItem, query: String): String? {
        val notes = task.notes
        val lowerNotes = notes?.lowercase()
        val lowerQuery = query.lowercase()
        val index = if (lowerNotes != null) lowerNotes.indexOf(lowerQuery) else -1
        if (notes == null || index < 0) {
            return null
        }

        val start = (index - SNIPPET_PADDING).coerceAtLeast(0)
        val end = (index + query.length + SNIPPET_PADDING).coerceAtMost(notes.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < notes.length) "…" else ""
        return "$prefix${notes.substring(start, end)}$suffix"
    }

    internal fun buildActions(currentQuery: String, enabledModules: Set<String>): List<SearchActionItem> {
        val trimmed = currentQuery.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<SearchActionItem>()

        // 1. Quick Task Creation Action
        results.add(
            SearchActionItem(
                id = "action_create",
                title = "Create task \"${currentQuery.trim()}\"",
                subtitle = "Quick Add",
                kind = SearchActionKind.CreateTask,
            ),
        )

        // 2. Navigation Actions matching query, gated by enabled modules
        STATIC_NAVIGATION_TARGETS.forEach { target ->
            val moduleAllowed = target.requiredModule == null || enabledModules.contains(target.requiredModule)
            if (moduleAllowed && target.matches(trimmed)) {
                results.add(
                    SearchActionItem(
                        id = target.id,
                        title = target.title,
                        subtitle = "Navigation",
                        kind = SearchActionKind.Navigate,
                        targetRoute = target.route,
                    ),
                )
            }
        }

        return results
    }

    private data class NavTarget(
        val id: String,
        val title: String,
        val keywords: List<String>,
        val route: OS11Route,
        val requiredModule: String? = null,
    ) {
        fun matches(query: String): Boolean = keywords.any { it.contains(query) || query.contains(it) }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 150L
        private const val SUBSCRIBE_TIMEOUT_MS = 5000L
        private const val MAX_SEARCH_RESULTS = 50
        private const val SNIPPET_PADDING = 24

        private val STATIC_NAVIGATION_TARGETS = listOf(
            NavTarget("nav_myday", "Go to My Day", listOf("my day", "today", "day"), OS11Route.MyDay, "my_day"),
            NavTarget("nav_inbox", "Go to Inbox", listOf("inbox", "incoming", "mail"), OS11Route.Inbox),
            NavTarget(
                "nav_planned",
                "Go to Planned",
                listOf("planned", "calendar", "schedule", "week"),
                OS11Route.Planned,
            ),
            NavTarget("nav_browse", "Go to Browse", listOf("browse", "projects", "areas", "all"), OS11Route.Browse),
            NavTarget(
                "nav_important",
                "Go to Important",
                listOf("important", "starred", "priority"),
                OS11Route.Important,
            ),
            NavTarget("nav_completed", "Go to Completed", listOf("completed", "done", "archive"), OS11Route.Completed),
            NavTarget("nav_trash", "Go to Trash", listOf("trash", "bin", "deleted"), OS11Route.Trash),
            NavTarget(
                "nav_settings",
                "Go to Settings",
                listOf("settings", "preferences", "config"),
                OS11Route.Settings,
            ),
            NavTarget(
                "nav_dashboard",
                "Go to Dashboard",
                listOf("dashboard", "stats", "overview"),
                OS11Route.Dashboard,
                requiredModule = "dashboard",
            ),
            NavTarget(
                "nav_pomodoro",
                "Go to Pomodoro",
                listOf("pomodoro", "timer", "focus"),
                OS11Route.Pomodoro,
                requiredModule = "pomodoro",
            ),
            NavTarget(
                "nav_goals",
                "Go to Goals",
                listOf("goals", "milestones", "targets"),
                OS11Route.Goals,
                requiredModule = "goals_habits",
            ),
            NavTarget(
                "nav_dayplan",
                "Go to Day Plan",
                listOf("day plan", "timeline", "agenda"),
                OS11Route.DayPlan,
                requiredModule = "agenda",
            ),
        )
    }
}
