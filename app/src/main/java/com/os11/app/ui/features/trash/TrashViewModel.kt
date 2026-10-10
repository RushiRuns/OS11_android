package com.os11.app.ui.features.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.task.TaskService
import com.os11.app.ui.features.task.TaskTreeFlattener
import com.os11.app.ui.features.task.TaskUiModel
import com.os11.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the trash screen and 30-day purge actions.
 */
@HiltViewModel
class TrashViewModel @Inject constructor(private val taskService: TaskService, private val clock: Clock) :
    ViewModel() {

    val trashedTasks: StateFlow<ImmutableList<TaskUiModel>> = taskService.observeTrashTasks()
        .map { tasks ->
            TaskTreeFlattener.flatten(
                tasks = tasks,
                todayIso = clock.today(),
            ).toImmutableList()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    fun restoreTask(taskId: String) {
        viewModelScope.launch {
            taskService.restoreTask(taskId)
        }
    }

    fun deletePermanently(taskId: String) {
        viewModelScope.launch {
            taskService.deletePermanently(taskId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            taskService.emptyTrash()
        }
    }
}
