package com.liukscot.reminders.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.liukscot.reminders.data.SnoozeChoice
import com.liukscot.reminders.data.SnoozeKind
import com.liukscot.reminders.data.snoozeChoices
import com.liukscot.reminders.wear.snooze.SnoozeConfirmation
import com.liukscot.reminders.wear.snooze.SnoozeScreen
import com.liukscot.reminders.wear.theme.RemindersWearTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

// Opened by the Snooze action on the watch's own reminder notification. The options come from the
// phone's snoozeChoices — the same function, shared as source, not a watch-sized subset of it.
class SnoozeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindersWearTheme {
                // Captured once so the labels and what eventually gets scheduled agree, even if
                // the user lingers past a minute boundary.
                val now = remember { LocalDateTime.now() }
                // ponytail: the phone's real quick-snooze delay arrives with the Data Layer
                // payload; until that lands this is the phone's own default.
                val choices = remember(now) { snoozeChoices(now, Duration.ofMinutes(10)) }
                var picked by remember { mutableStateOf<LocalDateTime?>(null) }

                val chosen = picked
                if (chosen == null) {
                    SnoozeScreen(
                        now = now,
                        choices = choices,
                        onPick = { choice -> picked = choice.resolve(now) },
                    )
                } else {
                    SnoozeConfirmation(at = chosen)
                    LaunchedEffect(chosen) {
                        delay(1_200)
                        finish()
                    }
                }
            }
        }
    }
}

// PickDateTime is the one option with no time of its own. Its picker is the next slice of #39;
// until then it falls back to the same delay the phone defaults to, rather than dismissing with
// nothing scheduled.
private fun SnoozeChoice.resolve(now: LocalDateTime): LocalDateTime =
    at ?: if (kind == SnoozeKind.PickDateTime) now.plusHours(1) else now
