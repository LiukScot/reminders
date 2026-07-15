package com.liukscot.reminders.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liukscot.reminders.RemindersApplication
import com.liukscot.reminders.data.RecurrenceRule
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.data.VoiceTaskDraft
import com.liukscot.reminders.notifications.ReminderScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListWithCount(val list: TaskList, val openTaskCount: Int)

data class ListsUiState(
    val lists: List<ListWithCount> = emptyList(),
    val allLists: List<TaskList> = emptyList(),
    val defaultListId: Long? = null,
)

class ListsViewModel(
    private val repository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    val uiState: StateFlow<ListsUiState> = combine(
        repository.lists,
        repository.openCountsByList,
        settingsRepository.defaultListId,
    ) { lists, counts, defaultListId ->
        val countByListId = counts.associate { it.listId to it.count }
        ListsUiState(
            lists = lists.map { ListWithCount(it, countByListId[it.id] ?: 0) },
            allLists = lists,
            defaultListId = defaultListId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListsUiState(),
    )

    fun addList(name: String, icon: String = "inbox") {
        viewModelScope.launch { repository.addList(name, icon) }
    }

    fun renameList(list: TaskList, newName: String) {
        viewModelScope.launch { repository.renameList(list, newName) }
    }

    fun deleteList(list: TaskList) {
        if (uiState.value.allLists.size <= 1) return
        viewModelScope.launch { repository.deleteList(list) }
    }

    fun addTask(
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
                null, title, notes, listId, tags, flagged, priority, dueAt, hasDueTime,
                recurrenceFreq, recurrenceInterval, recurrenceByDay, recurrenceAnchor,
            )
            reminderScheduler.schedule(saved)
        }
    }

    // Voice capture (#19): the AI-parsed draft, saved to the given list. Reuses addTask so the save
    // path and scheduling stay in one place; only the draft → fields mapping lives here.
    fun addVoiceTask(draft: VoiceTaskDraft, listId: Long) {
        val rule = draft.recurrence
        // A time or a recurrence needs a day to hang on: without an explicit date, anchor on today
        // (mirrors parseReminderText). Otherwise dueAt stays null and the time/recurrence are lost.
        val effectiveDate = draft.date ?: if (draft.time != null || rule != null) LocalDate.now() else null
        val dueAt = effectiveDate?.let { date ->
            LocalDateTime.of(date, draft.time ?: LocalTime.MIDNIGHT)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        addTask(
            title = draft.title,
            notes = null,
            listId = listId,
            tags = emptyList(),
            flagged = false,
            priority = 0,
            dueAt = dueAt,
            hasDueTime = draft.time != null,
            recurrenceFreq = rule?.frequency?.name,
            recurrenceInterval = rule?.interval ?: 1,
            recurrenceByDay = rule?.byDay?.let(RecurrenceRule::encodeByDay),
            recurrenceAnchor = if (rule != null) dueAt else null,
        )
    }
}

@Composable
fun rememberListsViewModel(): ListsViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel { ListsViewModel(app.repository, app.settingsRepository, app.reminderScheduler) }
}
