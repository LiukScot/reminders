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
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class DayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val tagsByTaskId: Map<Long, List<String>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val repository: RemindersRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<DayUiState> = selectedDate.flatMapLatest { date ->
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        combine(
            repository.tasksDueBetween(start, end),
            repository.openTasksDueBefore(start),
            repository.lists,
            repository.tagsByTaskId,
        ) { tasks, overdueTasks, lists, tagsByTaskId ->
            DayUiState(date, tasks, overdueTasks, lists, tagsByTaskId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DayUiState(),
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            repository.updateTask(updated)
            reminderScheduler.schedule(updated)
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

    internal fun moveTaskToSlot(task: Task, date: LocalDate, slot: DaySlot) {
        viewModelScope.launch {
            val dueAt = date
                .atTime(slot.scheduledTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val updated = task.copy(dueAt = dueAt, hasDueTime = true)
            repository.updateTask(updated)
            reminderScheduler.schedule(updated)
        }
    }
}

@Composable
fun rememberDayViewModel(): DayViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel { DayViewModel(app.repository, app.reminderScheduler) }
}
