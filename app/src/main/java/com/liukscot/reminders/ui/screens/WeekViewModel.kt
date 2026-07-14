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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekDay(
    val date: LocalDate,
    val tasks: List<Task>,
)

data class WeekUiState(
    val days: List<WeekDay> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val tagsByTaskId: Map<Long, List<String>> = emptyMap(),
)

// ponytail: a real infinite scroll needs incremental pagination as the user nears either edge.
// A generous fixed window (~8 months each way) is much less code and covers every realistic use
// of a personal reminders app; widen WINDOW_DAYS (or add pagination) if that ever falls short.
private const val WINDOW_DAYS = 120L

class WeekViewModel(
    private val repository: RemindersRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val rangeStart = LocalDate.now().minusDays(WINDOW_DAYS)
    private val rangeEnd = LocalDate.now().plusDays(WINDOW_DAYS)

    val uiState: StateFlow<WeekUiState> = run {
        val zone = ZoneId.systemDefault()
        val startMillis = rangeStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = rangeEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        combine(
            repository.tasksDueBetween(startMillis, endMillis),
            repository.lists,
            repository.tagsByTaskId,
        ) { tasks, lists, tagsByTaskId ->
            val tasksByDate = tasks.groupBy { it.dueDate(zone) }
            val days = generateSequence(rangeStart) { it.plusDays(1) }
                .takeWhile { it <= rangeEnd }
                .map { date ->
                    val dayTasks = tasksByDate[date]
                        .orEmpty()
                        .sortedWith(compareByDescending<Task> { it.hasDueTime }.thenBy { it.dueAt }.thenByDescending { it.priority })
                    WeekDay(date, dayTasks)
                }
                .toList()
            WeekUiState(days, lists, tagsByTaskId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeekUiState(),
    )

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

    internal fun moveTaskToDay(task: Task, newDate: LocalDate) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val newDueAt = if (task.hasDueTime && task.dueAt != null) {
                val time = Instant.ofEpochMilli(task.dueAt).atZone(zone).toLocalTime()
                newDate.atTime(time).atZone(zone).toInstant().toEpochMilli()
            } else {
                newDate.atStartOfDay(zone).toInstant().toEpochMilli()
            }
            val updated = task.copy(dueAt = newDueAt)
            repository.updateTask(updated)
            reminderScheduler.schedule(updated)
        }
    }
}

internal fun Task.dueDate(zone: ZoneId): LocalDate? =
    dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

@Composable
fun rememberWeekViewModel(): WeekViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel { WeekViewModel(app.repository, app.reminderScheduler) }
}
