package com.fitcoachpro.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fitcoach_prefs")

/**
 * Stores everything needed to reach the backend and run the reminder, all
 * user-editable from SettingsScreen. Nothing here is a secret baked into the
 * APK - the API_SHARED_SECRET the user enters is typed in once and stored in
 * this app's private DataStore, same trust boundary as any other app
 * preference (not encrypted at rest - see README's security note if you
 * want to harden this later with EncryptedSharedPreferences/Keystore).
 */
class PrefsRepository(private val context: Context) {

    private object Keys {
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val SHARED_SECRET = stringPreferencesKey("shared_secret")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    }

    val backendUrlFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.BACKEND_URL] ?: "" }

    val sharedSecretFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.SHARED_SECRET] ?: "" }

    val reminderHourFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.REMINDER_HOUR] ?: 7 } // default 7 AM

    val reminderMinuteFlow: Flow<Int> =
        context.dataStore.data.map { it[Keys.REMINDER_MINUTE] ?: 0 }

    val reminderEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.REMINDER_ENABLED] ?: false }

    suspend fun saveBackendConfig(backendUrl: String, sharedSecret: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKEND_URL] = backendUrl.trim()
            prefs[Keys.SHARED_SECRET] = sharedSecret.trim()
        }
    }

    suspend fun saveReminder(hour: Int, minute: Int, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
            prefs[Keys.REMINDER_ENABLED] = enabled
        }
    }

    /** One-shot reads for use outside Compose (e.g. from the WorkManager Worker). */
    suspend fun snapshotBackendUrl(): String = backendUrlFlow.first()
    suspend fun snapshotSharedSecret(): String = sharedSecretFlow.first()
    suspend fun snapshotReminderHour(): Int = reminderHourFlow.first()
    suspend fun snapshotReminderMinute(): Int = reminderMinuteFlow.first()
    suspend fun snapshotReminderEnabled(): Boolean = reminderEnabledFlow.first()
}
