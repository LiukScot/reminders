package com.liukscot.reminders.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liukscot.reminders.RemindersApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// AlarmManager alarms don't survive a reboot — Android drops every pending one.
// This re-arms an alarm for each task that still has a future due time.
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as RemindersApplication
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.pendingRemindersFrom(System.currentTimeMillis())
                    .forEach { app.reminderScheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
