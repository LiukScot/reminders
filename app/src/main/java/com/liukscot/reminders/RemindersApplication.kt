package com.liukscot.reminders

import android.app.Application
import com.liukscot.reminders.data.RemindersDatabase
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SecureKeyStore
import com.liukscot.reminders.data.SettingsRepository
import com.liukscot.reminders.data.VoiceTaskParsers
import com.liukscot.reminders.notifications.ReminderScheduler
import com.liukscot.reminders.notifications.createReminderNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RemindersApplication : Application() {
    val repository: RemindersRepository by lazy {
        RemindersRepository(RemindersDatabase.getInstance(this))
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }
    val secureKeyStore: SecureKeyStore by lazy { SecureKeyStore(this) }
    val voiceTaskParsers: VoiceTaskParsers by lazy { VoiceTaskParsers(secureKeyStore) }

    suspend fun quickSnoozeMillis(): Long =
        settingsRepository.quickSnoozeMinutes.first() * 60_000L

    // Lives as long as the process: the work below outlives any one screen.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel(this)
        // The seeded list can't make itself the default where it is created — that's raw SQL in
        // the database callback, with no reach into the settings store — so it happens on launch.
        // Without an explicit default, "no default" quietly resolves to whichever list sorts first
        // alphabetically, which moves under the user the moment they add a list.
        applicationScope.launch {
            if (settingsRepository.defaultListId.first() == null) {
                repository.lists.first().firstOrNull()?.let { settingsRepository.setDefaultListId(it.id) }
            }
        }
    }
}
