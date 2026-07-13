package com.liukscot.reminders.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liukscot.reminders.RemindersApplication
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.data.TaskList
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

    fun addTask(title: String, notes: String?, listId: Long, tags: List<String>) {
        viewModelScope.launch {
            val taskId = repository.addTask(
                Task(listId = listId, title = title, notes = notes, createdAt = System.currentTimeMillis()),
            )
            repository.setTags(taskId, tags)
        }
    }
}

@Composable
fun rememberListsViewModel(): ListsViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel { ListsViewModel(app.repository, app.settingsRepository) }
}
