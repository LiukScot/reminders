package com.liukscot.reminders

import android.app.Application
import com.liukscot.reminders.data.RemindersDatabase
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.notifications.ReminderScheduler
import com.liukscot.reminders.notifications.createReminderNotificationChannel

class RemindersApplication : Application() {
    val repository: RemindersRepository by lazy {
        val db = RemindersDatabase.getInstance(this)
        RemindersRepository(db.taskDao(), db.taskListDao(), db.tagDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel(this)
    }
}
