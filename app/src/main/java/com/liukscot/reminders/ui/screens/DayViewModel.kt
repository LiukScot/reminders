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
            reminderScheduler.schedule(repository.toggleComplete(task))
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
