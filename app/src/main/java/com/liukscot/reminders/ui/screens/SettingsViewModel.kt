package com.liukscot.reminders.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liukscot.reminders.RemindersApplication
import com.liukscot.reminders.data.AiProvider
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SecureKeyStore
import com.liukscot.reminders.data.DEFAULT_QUICK_SNOOZE_MINUTES
import com.liukscot.reminders.data.DEFAULT_START_PAGE_ROUTE
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.data.encodeBackup
import com.liukscot.reminders.data.decodeBackup
import com.liukscot.reminders.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

data class SettingsUiState(
    val lists: List<TaskList> = emptyList(),
    val defaultListId: Long? = null,
    val aiProvider: AiProvider = AiProvider.DEFAULT,
    val apiKeyHint: String? = null,
    val quickSnoozeMinutes: Long = DEFAULT_QUICK_SNOOZE_MINUTES,
    val startPageRoute: String = DEFAULT_START_PAGE_ROUTE,
) {
    val defaultListName: String? get() = lists.firstOrNull { it.id == defaultListId }?.name
}

// The choices behind the notification's one-tap snooze — a short menu, not a free number field.
val QUICK_SNOOZE_CHOICES = listOf(10L, 15L, 30L, 60L, 120L)

fun quickSnoozeLabel(minutes: Long): String =
    if (minutes < 60) "$minutes min" else "${minutes / 60} h"

private data class Prefs(
    val aiProvider: AiProvider,
    val apiKeyHint: String?,
    val quickSnoozeMinutes: Long,
    val startPageRoute: String,
)

private const val TAG = "SettingsViewModel"

class SettingsViewModel(
    private val repository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
    private val keyStore: SecureKeyStore,
    private val contentResolver: ContentResolver,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    // Export and restore have no on-screen state of their own — they report one line and are done.
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun clearMessage() {
        _message.value = null
    }

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

    // Grouped because Kotlin's typed combine tops out at five flows; the settings scalars fold into
    // one so the outer combine stays within that.
    private val prefs = combine(
        settingsRepository.aiProvider,
        apiKeyHint,
        settingsRepository.quickSnoozeMinutes,
        settingsRepository.startPageRoute,
    ) { aiProvider, hint, quickSnooze, startPage -> Prefs(aiProvider, hint, quickSnooze, startPage) }

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.lists,
        settingsRepository.defaultListId,
        prefs,
    ) { lists, defaultListId, p ->
        SettingsUiState(lists, defaultListId, p.aiProvider, p.apiKeyHint, p.quickSnoozeMinutes, p.startPageRoute)
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

    fun setQuickSnoozeMinutes(minutes: Long) {
        viewModelScope.launch { settingsRepository.setQuickSnoozeMinutes(minutes) }
    }

    fun setStartPage(route: String) {
        viewModelScope.launch { settingsRepository.setStartPageRoute(route) }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch {
            val provider = uiState.value.aiProvider
            withContext(Dispatchers.IO) { keyStore.setApiKey(provider, key) }
            apiKeyHint.value = key.trim().ifBlank { null }?.let(::maskKey)
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val backup = repository.exportBackup()
            val text = encodeBackup(backup, exportedAt = System.currentTimeMillis())
            _message.value = try {
                withContext(Dispatchers.IO) {
                    // The picker guarantees the document exists; a null stream means it vanished.
                    contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                        ?: throw IOException("could not open $uri for writing")
                }
                "Exported ${backup.tasks.size} reminders"
            } catch (e: IOException) {
                Log.w(TAG, "export failed", e)
                "Export failed: ${e.message}"
            }
        }
    }

    // The file comes from the system picker, so it is whatever the user tapped — unreadable,
    // not JSON, or a backup from a future version are all normal outcomes, not crashes.
    fun restoreFrom(uri: Uri) {
        viewModelScope.launch {
            val backup = try {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                        ?: throw IOException("could not open $uri for reading")
                }
                decodeBackup(text)
            } catch (e: IOException) {
                Log.w(TAG, "restore failed to read the file", e)
                _message.value = "Restore failed: ${e.message}"
                return@launch
            } catch (e: JSONException) {
                Log.w(TAG, "restore failed to parse the file", e)
                _message.value = "Restore failed: not a valid backup file"
                return@launch
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "restore rejected the file", e)
                _message.value = "Restore failed: ${e.message}"
                return@launch
            }

            // Alarms are keyed by task id, and the wipe frees every id for the file's tasks to take
            // over — so pending ones would fire for reminders that no longer exist, under a title
            // now belonging to something else. Clear them all, then re-arm from the restored rows.
            repository.allTasks().forEach { reminderScheduler.cancel(it.id) }
            repository.restoreBackup(backup)
            repository.allTasks().forEach { reminderScheduler.schedule(it) }
            _message.value = "Restored ${backup.tasks.size} reminders"
        }
    }
}

private fun maskKey(key: String): String = "••••" + key.takeLast(4)

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel {
        SettingsViewModel(
            app.repository,
            app.settingsRepository,
            app.secureKeyStore,
            app.contentResolver,
            app.reminderScheduler,
        )
    }
}
