package com.liukscot.reminders

import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.liukscot.reminders.data.SnoozeChoice
import com.liukscot.reminders.data.SnoozeKind
import com.liukscot.reminders.data.snoozeChoices
import com.liukscot.reminders.notifications.EXTRA_TASK_ID
import com.liukscot.reminders.notifications.EXTRA_TASK_TITLE
import com.liukscot.reminders.ui.screens.SnoozeScreen
import com.liukscot.reminders.ui.theme.RemindersTheme
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

// Opened directly by the notification's "Snooze…" action — a getActivity PendingIntent, not a
// receiver hop, so it is a legal target on Android 12+. Renders over the shade as a bottom sheet:
// pick an option, it reschedules, and the activity closes.
class SnoozeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        if (taskId == -1L) {
            finish()
            return
        }
        val app = application as RemindersApplication
        enableEdgeToEdge()
        setContent {
            RemindersTheme {
                // now is captured once so the labels and what actually gets scheduled agree, even if
                // the user lingers over the sheet past a minute boundary.
                val now = remember { LocalDateTime.now() }
                var quickMinutes by remember { mutableStateOf<Long?>(null) }
                val context = LocalContext.current

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    quickMinutes = app.quickSnoozeMillis() / 60_000L
                }

                // Bottom-aligned sheet over a scrim; tapping the scrim backs out.
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    color = Color.Transparent,
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Surface(
                            modifier = Modifier.wrapContentHeight(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            val minutes = quickMinutes
                            if (minutes != null) {
                                SnoozeScreen(
                                    title = title,
                                    now = now,
                                    choices = snoozeChoices(now, Duration.ofMinutes(minutes)),
                                    onPick = { choice ->
                                        if (choice.kind == SnoozeKind.PickDateTime) {
                                            pickDateTime(now) { picked -> apply(app, taskId, picked) }
                                        } else {
                                            apply(app, taskId, choice.at ?: return@SnoozeScreen)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun apply(app: RemindersApplication, taskId: Long, at: LocalDateTime) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        lifecycleScope.launch {
            app.repository.snoozeTask(taskId, millis)?.let(app.reminderScheduler::schedule)
            getSystemService(NotificationManager::class.java).cancel(taskId.toInt())
            finish()
        }
    }

    // Native date then time dialog — less to maintain than reusing the in-app sheet, and it is a
    // throwaway path for the arbitrary case the presets don't cover.
    private fun pickDateTime(now: LocalDateTime, onPicked: (LocalDateTime) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute -> onPicked(LocalDateTime.of(year, month + 1, day, hour, minute)) },
                    now.hour,
                    now.minute,
                    true,
                ).show()
            },
            now.year,
            now.monthValue - 1,
            now.dayOfMonth,
        ).show()
    }
}
