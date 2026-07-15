package com.liukscot.reminders.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

// Per-provider API keys, encrypted at rest via the Android Keystore (see architecture.md → Voice/AI
// for why keys live on-device rather than behind a proxy). One entry per AiProvider.
class SecureKeyStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "secure_keys",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun apiKey(provider: AiProvider): String? = prefs.getString(provider.name, null)?.ifBlank { null }

    fun setApiKey(provider: AiProvider, key: String) {
        prefs.edit().putString(provider.name, key.trim()).apply()
    }
}
