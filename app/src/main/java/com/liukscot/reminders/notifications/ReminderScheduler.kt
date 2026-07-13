package com.liukscot.reminders.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.liukscot.reminders.data.Task

const val EXTRA_TASK_ID = "task_id"
const val EXTRA_LIST_ID = "list_id"
const val EXTRA_TASK_TITLE = "task_title"

// One task <-> one alarm, keyed by task id as the PendingIntent request code —
// rescheduling (cancel then set) is how an edited due time is applied.
class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(task: Task) {
        cancel(task.id)
        val dueAt = task.dueAt
        if (dueAt == null || !task.hasDueTime || task.completed || dueAt <= System.currentTimeMillis()) return

        val pendingIntent = pendingIntentFor(task.id, task.listId, task.title)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
        } else {
            // ponytail: no exact-alarm permission — an approximate fire time
            // beats silently dropping the reminder. Upgrade path: prompt the
            // user to grant SCHEDULE_EXACT_ALARM if this matters more later.
            alarmManager.set(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntentFor(taskId, listId = 0, title = ""))
    }

    private fun pendingIntentFor(taskId: Long, listId: Long, title: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_LIST_ID, listId)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
