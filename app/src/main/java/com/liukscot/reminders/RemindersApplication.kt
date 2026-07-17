package com.liukscot.reminders

import android.app.Application
import com.liukscot.reminders.data.RemindersDatabase
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SecureKeyStore
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.VoiceTaskParsers
import com.liukscot.reminders.notifications.ReminderScheduler
import com.liukscot.reminders.notifications.createReminderNotificationChannel

class RemindersApplication : Application() {
    val repository: RemindersRepository by lazy {
        RemindersRepository(RemindersDatabase.getInstance(this))
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }
    val secureKeyStore: SecureKeyStore by lazy { SecureKeyStore(this) }
    val voiceTaskParsers: VoiceTaskParsers by lazy { VoiceTaskParsers(secureKeyStore) }

    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel(this)
    }
}
