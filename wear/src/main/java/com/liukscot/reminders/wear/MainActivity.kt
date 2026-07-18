package com.liukscot.reminders.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.liukscot.reminders.wear.theme.RemindersWearTheme
import com.liukscot.reminders.wear.voice.VoiceScreen
import com.liukscot.reminders.wear.voice.VoiceUiState

// v1's launcher entry and the complication's target both land here: idle voice is the whole app.
//
// The states are still driven locally. Speech capture and the Data Layer relay to the phone are
// the next slice of #39 — until they land, tapping through walks the five states so the screens
// can be checked on real glass against mockups/wear/index.html.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindersWearTheme {
                var state by remember { mutableStateOf<VoiceUiState>(VoiceUiState.Idle) }
                VoiceScreen(
                    state = state,
                    onMicTap = { state = VoiceUiState.Listening(DEMO_TRANSCRIPT) },
                    onStopListening = { state = VoiceUiState.Processing(DEMO_TRANSCRIPT) },
                    onSave = { finish() },
                    onRetry = { state = VoiceUiState.Idle },
                    onDiscard = { finish() },
                )
                // Stands in for the round trip to the phone, so Processing is a state you can
                // actually see instead of one that flashes past.
                LaunchedEffect(state) {
                    if (state is VoiceUiState.Processing) {
                        delay(1_500)
                        state = VoiceUiState.Result(
                            title = "Call mum",
                            due = "Tomorrow, 18:00",
                            list = "Personal",
                        )
                    }
                }
            }
        }
    }
}

private const val DEMO_TRANSCRIPT = "Call mum tomorrow at six"
