package com.liukscot.reminders.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liukscot.reminders.RemindersApplication
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskGroup(val task: Task, val subtasks: List<Task>, val tags: List<String>)

data class TaskListDetailUiState(
    val list: TaskList? = null,
    val lists: List<TaskList> = emptyList(),
    val open: List<TaskGroup> = emptyList(),
    val completed: List<TaskGroup> = emptyList(),
)

class TaskListDetailViewModel(
    private val repository: RemindersRepository,
    private val reminderScheduler: ReminderScheduler,
    private val listId: Long,
) : ViewModel() {
    val uiState: StateFlow<TaskListDetailUiState> = combine(
        repository.lists,
        repository.tasksIn(listId),
        repository.tagsByTaskId,
    ) { lists, tasks, tagsByTaskId ->
        val childrenByParent = tasks.filter { it.parentId != null }.groupBy { it.parentId }
        val groups = tasks
            .filter { it.parentId == null }
            .map { TaskGroup(it, childrenByParent[it.id] ?: emptyList(), tagsByTaskId[it.id] ?: emptyList()) }
        TaskListDetailUiState(
            list = lists.firstOrNull { it.id == listId },
            lists = lists,
            open = groups.filter { !it.task.completed },
            completed = groups.filter { it.task.completed },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListDetailUiState(),
    )

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
    ) {
        viewModelScope.launch {
            val saved = if (existing != null) {
                existing.copy(
                    title = title,
                    notes = notes,
                    listId = listId,
                    flagged = flagged,
                    priority = priority,
                    dueAt = dueAt,
                    hasDueTime = hasDueTime,
                ).also { repository.updateTask(it) }
            } else {
                val newTask = Task(
                    listId = listId,
                    title = title,
                    notes = notes,
                    flagged = flagged,
                    priority = priority,
                    dueAt = dueAt,
                    hasDueTime = hasDueTime,
                    createdAt = System.currentTimeMillis(),
                )
                newTask.copy(id = repository.addTask(newTask))
            }
            repository.setTags(saved.id, tags)
            reminderScheduler.schedule(saved)
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            repository.updateTask(updated)
            reminderScheduler.schedule(updated)
        }
    }

    fun renameList(newName: String) {
        val current = uiState.value.list ?: return
        viewModelScope.launch { repository.renameList(current, newName) }
    }

    fun deleteList(onDeleted: () -> Unit) {
        val current = uiState.value.list ?: return
        if (uiState.value.lists.size <= 1) return
        viewModelScope.launch {
            repository.deleteList(current)
            onDeleted()
        }
    }
}

@Composable
fun rememberTaskListDetailViewModel(listId: Long): TaskListDetailViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel(key = "list-detail-$listId") {
        TaskListDetailViewModel(app.repository, app.reminderScheduler, listId)
    }
}
