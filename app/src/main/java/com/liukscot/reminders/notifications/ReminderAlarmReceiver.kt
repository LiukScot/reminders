package com.liukscot.reminders.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.liukscot.reminders.MainActivity
import com.liukscot.reminders.R

const val REMINDER_NOTIFICATION_CHANNEL_ID = "reminders_due"

fun createReminderNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        REMINDER_NOTIFICATION_CHANNEL_ID,
        "Reminders",
        NotificationManager.IMPORTANCE_HIGH,
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val listId = intent.getLongExtra(EXTRA_LIST_ID, -1)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        if (taskId == -1L) return

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LIST_ID, listId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, REMINDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(title)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // The three action slots Android allows, in order of how often they're reached for.
            .addAction(0, "Done", actionPendingIntent(context, taskId, ACTION_COMPLETE))
            .addAction(0, "Snooze", actionPendingIntent(context, taskId, ACTION_QUICK_SNOOZE))
            .addAction(0, "Snooze…", snoozePickerPendingIntent(context, taskId, title))
            .build()

        context.getSystemService(NotificationManager::class.java).notify(taskId.toInt(), notification)
    }
}

// Silent actions (Done, quick Snooze) fire the broadcast receiver. Request code mixes task id and
// action so a task's Done and Snooze buttons don't collide on the same PendingIntent.
private fun actionPendingIntent(context: Context, taskId: Long, action: String): PendingIntent {
    val intent = Intent(context, ReminderActionReceiver::class.java).apply {
        this.action = action
        putExtra(EXTRA_TASK_ID, taskId)
    }
    return PendingIntent.getBroadcast(
        context,
        (taskId.toInt() * 31) + action.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

// "Snooze…" opens a screen, so it goes straight to the activity — a receiver that then started it
// would be an illegal trampoline on Android 12+.
private fun snoozePickerPendingIntent(context: Context, taskId: Long, title: String): PendingIntent {
    val intent = Intent(context, com.liukscot.reminders.SnoozeActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra(EXTRA_TASK_ID, taskId)
        putExtra(EXTRA_TASK_TITLE, title)
    }
    return PendingIntent.getActivity(
        context,
        taskId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
