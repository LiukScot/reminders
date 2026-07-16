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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchGroup(val listName: String, val tasks: List<Task>)

data class SearchUiState(
    val query: String = "",
    val groups: List<SearchGroup> = emptyList(),
    // Tapping a hit opens the edit sheet, which needs every list to choose from and the task's tags.
    val lists: List<TaskList> = emptyList(),
    val tagsByTaskId: Map<Long, List<String>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: RemindersRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = query.flatMapLatest { raw ->
        val trimmed = raw.trim()
        // A blank query means "nothing searched yet", not "match everything".
        val hits = if (trimmed.isEmpty()) flowOf(emptyList()) else repository.searchTasks(trimmed)
        combine(hits, repository.lists, repository.tagsByTaskId) { tasks, lists, tagsByTaskId ->
            // Ref: mockup groups hits by list and drops lists with no hits.
            val groups = lists.mapNotNull { list ->
                tasks.filter { it.listId == list.id }
                    .takeIf { it.isNotEmpty() }
                    ?.let { SearchGroup(list.name, it) }
            }
            SearchUiState(raw, groups, lists, tagsByTaskId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
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
}

@Composable
fun rememberSearchViewModel(): SearchViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel { SearchViewModel(app.repository, app.reminderScheduler) }
}
