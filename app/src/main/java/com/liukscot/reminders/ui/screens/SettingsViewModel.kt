package com.liukscot.reminders.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liukscot.reminders.RemindersApplication
import com.liukscot.reminders.data.AiProvider
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SecureKeyStore
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.TaskList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val lists: List<TaskList> = emptyList(),
    val defaultListId: Long? = null,
    val aiProvider: AiProvider = AiProvider.DEFAULT,
    val apiKeyHint: String? = null,
) {
    val defaultListName: String? get() = lists.firstOrNull { it.id == defaultListId }?.name
}

class SettingsViewModel(
    private val repository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
    private val keyStore: SecureKeyStore,
) : ViewModel() {
    // The stored key isn't reactive (EncryptedSharedPreferences), so mirror a masked hint of it into
    // a flow: reloaded whenever the selected provider changes and after the user saves a new key.
    private val apiKeyHint = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            settingsRepository.aiProvider.collect { provider ->
                apiKeyHint.value = withContext(Dispatchers.IO) { keyStore.apiKey(provider) }?.let(::maskKey)
            }
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.lists,
        settingsRepository.defaultListId,
        settingsRepository.aiProvider,
        apiKeyHint,
    ) { lists, defaultListId, aiProvider, hint ->
        SettingsUiState(lists, defaultListId, aiProvider, hint)
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

    fun setApiKey(key: String) {
        viewModelScope.launch {
            val provider = uiState.value.aiProvider
            withContext(Dispatchers.IO) { keyStore.setApiKey(provider, key) }
            apiKeyHint.value = key.trim().ifBlank { null }?.let(::maskKey)
        }
    }
}

private fun maskKey(key: String): String = "••••" + key.takeLast(4)

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel { SettingsViewModel(app.repository, app.settingsRepository, app.secureKeyStore) }
}
