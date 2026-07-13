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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskGroup(val task: Task, val subtasks: List<Task>)

data class TaskListDetailUiState(
    val list: TaskList? = null,
    val lists: List<TaskList> = emptyList(),
    val open: List<TaskGroup> = emptyList(),
    val completed: List<TaskGroup> = emptyList(),
)

class TaskListDetailViewModel(
    private val repository: RemindersRepository,
    private val listId: Long,
) : ViewModel() {
    val uiState: StateFlow<TaskListDetailUiState> = combine(
        repository.lists,
        repository.tasksIn(listId),
    ) { lists, tasks ->
        val childrenByParent = tasks.filter { it.parentId != null }.groupBy { it.parentId }
        val groups = tasks
            .filter { it.parentId == null }
            .map { TaskGroup(it, childrenByParent[it.id] ?: emptyList()) }
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

    fun saveTask(existing: Task?, title: String, notes: String?, listId: Long) {
        viewModelScope.launch {
            if (existing != null) {
                repository.updateTask(existing.copy(title = title, notes = notes, listId = listId))
            } else {
                repository.addTask(
                    Task(listId = listId, title = title, notes = notes, createdAt = System.currentTimeMillis()),
                )
            }
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch { repository.updateTask(task.copy(completed = !task.completed)) }
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
    return viewModel(key = "list-detail-$listId") { TaskListDetailViewModel(app.repository, listId) }
}
