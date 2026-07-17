package com.liukscot.reminders.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val defaultListIdKey = longPreferencesKey("default_list_id")
    private val aiProviderKey = stringPreferencesKey("ai_provider")
    private val quickSnoozeMinutesKey = longPreferencesKey("quick_snooze_minutes")
    private val startPageKey = stringPreferencesKey("start_page")

    val defaultListId: Flow<Long?> = context.settingsDataStore.data.map { it[defaultListIdKey] }

    // The tab the app opens on, as its nav route. Stored as a plain string so this layer stays
    // clear of the UI's Destination enum; the UI validates it against the real tabs. Default: Lists.
    val startPageRoute: Flow<String> =
        context.settingsDataStore.data.map { it[startPageKey] ?: DEFAULT_START_PAGE_ROUTE }

    val aiProvider: Flow<AiProvider> = context.settingsDataStore.data.map { AiProvider.fromKey(it[aiProviderKey]) }

    // The delay behind the notification's one-tap snooze button. Defaults to an hour — the value
    // the picker's +1h uses, so the button matches the option most people reach for.
    val quickSnoozeMinutes: Flow<Long> =
        context.settingsDataStore.data.map { it[quickSnoozeMinutesKey] ?: DEFAULT_QUICK_SNOOZE_MINUTES }

    suspend fun setDefaultListId(id: Long) {
        context.settingsDataStore.edit { it[defaultListIdKey] = id }
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.settingsDataStore.edit { it[aiProviderKey] = provider.name }
    }

    suspend fun setQuickSnoozeMinutes(minutes: Long) {
        context.settingsDataStore.edit { it[quickSnoozeMinutesKey] = minutes }
    }

    suspend fun setStartPageRoute(route: String) {
        context.settingsDataStore.edit { it[startPageKey] = route }
    }
}

const val DEFAULT_QUICK_SNOOZE_MINUTES = 60L
const val DEFAULT_START_PAGE_ROUTE = "lists"
