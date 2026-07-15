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

    val defaultListId: Flow<Long?> = context.settingsDataStore.data.map { it[defaultListIdKey] }

    val aiProvider: Flow<AiProvider> = context.settingsDataStore.data.map { AiProvider.fromKey(it[aiProviderKey]) }

    suspend fun setDefaultListId(id: Long) {
        context.settingsDataStore.edit { it[defaultListIdKey] = id }
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.settingsDataStore.edit { it[aiProviderKey] = provider.name }
    }
}
