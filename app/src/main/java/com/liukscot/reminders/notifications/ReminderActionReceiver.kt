package com.liukscot.reminders.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liukscot.reminders.RemindersApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val ACTION_COMPLETE = "com.liukscot.reminders.action.COMPLETE"
const val ACTION_QUICK_SNOOZE = "com.liukscot.reminders.action.QUICK_SNOOZE"

// The notification's silent actions — Done and the one-tap snooze. Neither opens a screen, so a
// receiver is the right home (an action that DID open UI would be an illegal trampoline on Android
// 12+; that one is a getActivity PendingIntent straight to SnoozeActivity instead).
class ReminderActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1L) return
        val action = intent.action ?: return

        val app = context.applicationContext as RemindersApplication
        // goAsync keeps the receiver alive past onReceive so the database work can finish; the
        // 10-second window it grants is ample for one row.
        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    ACTION_COMPLETE -> {
                        app.repository.completeById(taskId)?.let(app.reminderScheduler::schedule)
                    }
                    ACTION_QUICK_SNOOZE -> {
                        val until = System.currentTimeMillis() + app.quickSnoozeMillis()
                        app.repository.snoozeTask(taskId, until)?.let(app.reminderScheduler::schedule)
                    }
                }
                context.getSystemService(NotificationManager::class.java).cancel(taskId.toInt())
            } finally {
                pending.finish()
            }
        }
    }
}
