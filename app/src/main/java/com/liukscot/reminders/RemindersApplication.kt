package com.liukscot.reminders

import android.app.Application
import com.liukscot.reminders.data.RemindersDatabase
import com.liukscot.reminders.data.RemindersRepository
import com.liukscot.reminders.data.SettingsRepository

class RemindersApplication : Application() {
    val repository: RemindersRepository by lazy {
        val db = RemindersDatabase.getInstance(this)
        RemindersRepository(db.taskDao(), db.taskListDao(), db.tagDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
