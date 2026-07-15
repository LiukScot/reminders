package com.liukscot.reminders.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liukscot.reminders.RemindersApplication
import com.liukscot.reminders.data.AiProvider
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.TaskList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lists: List<TaskList> = emptyList(),
    val defaultListId: Long? = null,
    val aiProvider: AiProvider = AiProvider.DEFAULT,
) {
    val defaultListName: String? get() = lists.firstOrNull { it.id == defaultListId }?.name
}

class SettingsViewModel(
    private val repository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        repository.lists,
        settingsRepository.defaultListId,
        settingsRepository.aiProvider,
    ) { lists, defaultListId, aiProvider ->
        SettingsUiState(lists, defaultListId, aiProvider)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setDefaultList(id: Long) {
        viewModelScope.launch { settingsRepository.setDefaultListId(id) }
    }

    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch { settingsRepository.setAiProvider(provider) }
    }
}

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel { SettingsViewModel(app.repository, app.settingsRepository) }
}
