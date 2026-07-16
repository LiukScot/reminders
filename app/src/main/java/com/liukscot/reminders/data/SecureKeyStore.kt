package com.liukscot.reminders.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.GeneralSecurityException

private const val PREFS_NAME = "secure_keys"

// Per-provider API keys, encrypted at rest via the Android Keystore (see architecture.md → Voice/AI
// for why keys live on-device rather than behind a proxy). One entry per AiProvider.
class SecureKeyStore(context: Context) {
    private val prefs: SharedPreferences = openOrReset(context)

    fun apiKey(provider: AiProvider): String? = prefs.getString(provider.name, null)?.ifBlank { null }

    fun setApiKey(provider: AiProvider, key: String) {
        prefs.edit().putString(provider.name, key.trim()).apply()
    }
}

// The keystore master key does not survive an uninstall or a keystore reset, while the encrypted
// prefs file can come back from a device backup. The keys are then undecryptable for good, and
// EncryptedSharedPreferences.create throws — on every launch, since this sits behind the app's
// startup. Drop the unreadable file and start over: the user re-enters the API key, which beats an
// app that cannot open at all.
private fun openOrReset(context: Context): SharedPreferences = try {
    createEncryptedPrefs(context)
} catch (e: GeneralSecurityException) {
    Log.w("SecureKeyStore", "Stored API keys are undecryptable, resetting them", e)
    context.deleteSharedPreferences(PREFS_NAME)
    createEncryptedPrefs(context)
} catch (e: java.io.IOException) {
    Log.w("SecureKeyStore", "Stored API keys are unreadable, resetting them", e)
    context.deleteSharedPreferences(PREFS_NAME)
    createEncryptedPrefs(context)
}

private fun createEncryptedPrefs(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
    PREFS_NAME,
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)
