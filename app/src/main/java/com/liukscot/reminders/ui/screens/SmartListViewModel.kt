package com.liukscot.reminders.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liukscot.reminders.RemindersApplication
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.SmartList
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SmartListUiState(
    val tasks: List<TaskGroup> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    // A smart list is a filter with no list of its own, so a reminder added from here lands in the
    // default list (or the first, if none is set) — same target as the Lists screen's add button.
    val newTaskListId: Long? = null,
)

class SmartListViewModel(
    private val repository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    smartList: SmartList,
) : ViewModel() {
    val uiState: StateFlow<SmartListUiState> = combine(
        repository.tasksIn(smartList),
        repository.lists,
        repository.tagsByTaskId,
        settingsRepository.defaultListId,
    ) { tasks, lists, tagsByTaskId, defaultListId ->
        // A smart list is a filtered slice, so a sub-task can match while its parent doesn't. Nest
        // only under a parent that survived the filter — the rest stand on their own, rather than
        // vanishing with the parent that isn't here to draw them.
        val presentIds = tasks.mapTo(mutableSetOf()) { it.id }
        val childrenByParent = tasks.filter { it.parentId in presentIds }.groupBy { it.parentId }
        SmartListUiState(
            tasks = tasks
                .filter { it.parentId !in presentIds }
                .map { TaskGroup(it, childrenByParent[it.id].orEmpty(), tagsByTaskId[it.id].orEmpty()) },
            lists = lists,
            newTaskListId = defaultListId ?: lists.firstOrNull()?.id,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SmartListUiState(),
    )

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            reminderScheduler.schedule(repository.toggleComplete(task))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            // Cancel first: the row is about to stop existing, and an alarm outlives it otherwise.
            reminderScheduler.cancel(task.id)
            repository.deleteTask(task)
        }
    }

    fun saveTask(
        existing: Task?,
        title: String,
        notes: String?,
        listId: Long,
        tags: List<String>,
        flagged: Boolean,
        priority: Int,
        dueAt: Long?,
        hasDueTime: Boolean,
        recurrenceFreq: String?,
        recurrenceInterval: Int,
        recurrenceByDay: String?,
        recurrenceAnchor: Long?,
    ) {
        viewModelScope.launch {
            val saved = repository.saveTask(
                existing, title, notes, listId, tags, flagged, priority, dueAt, hasDueTime,
                recurrenceFreq, recurrenceInterval, recurrenceByDay, recurrenceAnchor,
            )
            reminderScheduler.schedule(saved)
        }
    }
}

@Composable
fun rememberSmartListViewModel(smartList: SmartList): SmartListViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel(key = "smart-list-${smartList.name}") {
        SmartListViewModel(app.repository, app.settingsRepository, app.reminderScheduler, smartList)
    }
}
