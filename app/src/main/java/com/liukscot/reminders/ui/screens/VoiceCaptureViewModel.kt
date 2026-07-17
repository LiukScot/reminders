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
import com.liukscot.reminders.data.SpeechTranscriber
import com.liukscot.reminders.data.VoiceTaskDraft
import com.liukscot.reminders.data.VoiceTaskParsers
import com.liukscot.reminders.notifications.ReminderScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface VoiceCaptureState {
    data object Idle : VoiceCaptureState
    data class Listening(val partial: String) : VoiceCaptureState
    data object Processing : VoiceCaptureState
    data class Result(
        val draft: VoiceTaskDraft,
        val transcript: String,
        val listId: Long?,
        val listName: String?,
    ) : VoiceCaptureState
    data object MissingKey : VoiceCaptureState
    data class Error(val message: String) : VoiceCaptureState
}

class VoiceCaptureViewModel(
    private val transcriber: SpeechTranscriber,
    private val parsers: VoiceTaskParsers,
    private val settingsRepository: SettingsRepository,
    private val repository: RemindersRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val _state = MutableStateFlow<VoiceCaptureState>(VoiceCaptureState.Idle)
    val state: StateFlow<VoiceCaptureState> = _state.asStateFlow()
    private var parseJob: Job? = null

    fun start() {
        if (!transcriber.isAvailable) {
            _state.value = VoiceCaptureState.Error("Speech recognition isn't available on this device.")
            return
        }
        _state.value = VoiceCaptureState.Listening("")
        transcriber.start(
            onPartial = { partial -> _state.value = VoiceCaptureState.Listening(partial) },
            onFinal = { transcript -> parse(transcript) },
            onError = { message -> _state.value = VoiceCaptureState.Error(message) },
        )
    }

    // User tapped the mic to finish — ask the recognizer to finalize (its onResults drives parse()).
    fun stopListening() {
        if (_state.value is VoiceCaptureState.Listening) transcriber.stop()
    }

    fun retry() {
        parseJob?.cancel()
        transcriber.destroy()
        start()
    }

    fun dismiss() {
        parseJob?.cancel()
        transcriber.destroy()
        _state.value = VoiceCaptureState.Idle
    }

    // Saves the confirmed draft to its resolved list and closes the overlay. Self-contained so the
    // mic button drops into any screen (Lists/Day/Week) without each wiring its own save path. A
    // time or recurrence with no explicit date anchors on today, else dueAt stays null and is lost.
    fun addReminder() {
        val result = _state.value as? VoiceCaptureState.Result ?: return
        val listId = result.listId ?: return
        val draft = result.draft
        val rule = draft.recurrence
        val effectiveDate = draft.date ?: if (draft.time != null || rule != null) LocalDate.now() else null
        val dueAt = effectiveDate?.let {
            LocalDateTime.of(it, draft.time ?: LocalTime.MIDNIGHT)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        viewModelScope.launch {
            val saved = repository.saveTask(
                null, draft.title, null, listId, emptyList(), false, 0, dueAt, draft.time != null,
                rule?.frequency?.name, rule?.interval ?: 1, rule?.byDay?.let(RecurrenceRule::encodeByDay),
                if (rule != null) dueAt else null,
            )
            reminderScheduler.schedule(saved)
        }
        dismiss()
    }

    private fun parse(transcript: String) {
        if (transcript.isBlank()) {
            _state.value = VoiceCaptureState.Error("Didn't catch that — try again.")
            return
        }
        _state.value = VoiceCaptureState.Processing
        parseJob = viewModelScope.launch {
            val provider = settingsRepository.aiProvider.first()
            val parser = parsers.forProvider(provider)
            if (parser == null) {
                _state.value = VoiceCaptureState.MissingKey
                return@launch
            }
            val lists = repository.lists.first()
            _state.value = try {
                val draft = parser.parse(transcript, LocalDateTime.now(), lists.map { it.name })
                // The AI names a list; match it (loosely) to a real one, else fall back to the default.
                val picked = draft.list?.let { name -> lists.firstOrNull { it.name.equals(name, ignoreCase = true) } }
                val fallback = lists.firstOrNull { it.id == settingsRepository.defaultListId.first() }
                    ?: lists.firstOrNull()
                val resolved = picked ?: fallback
                VoiceCaptureState.Result(draft, transcript, resolved?.id, resolved?.name)
            } catch (e: Exception) {
                VoiceCaptureState.Error(e.message ?: "Couldn't reach the AI provider.")
            }
        }
    }

    override fun onCleared() {
        transcriber.destroy()
    }
}

@Composable
fun rememberVoiceCaptureViewModel(): VoiceCaptureViewModel {
    val app = LocalContext.current.applicationContext as RemindersApplication
    return viewModel {
        VoiceCaptureViewModel(
            SpeechTranscriber(app), app.voiceTaskParsers, app.settingsRepository, app.repository, app.reminderScheduler,
        )
    }
}
