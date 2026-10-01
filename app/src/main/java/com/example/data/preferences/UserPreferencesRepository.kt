package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "apunta_preferences")

data class UserPreferences(
    val userName: String = "",
    val hasCompletedOnboarding: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val defaultLeadTimeMinutes: Int = 0,
    val staggeredAlerts: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val morningSummaryEnabled: Boolean = true,
    val morningSummaryTime: String = "08:00",
    val onDeviceVoiceOnly: Boolean = true,
    val voiceDialect: String = "es-SV" // es-SV or es-419
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_LEAD_TIME_MINUTES = intPreferencesKey("default_lead_time_minutes")
        val STAGGERED_ALERTS = booleanPreferencesKey("staggered_alerts")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val MORNING_SUMMARY_ENABLED = booleanPreferencesKey("morning_summary_enabled")
        val MORNING_SUMMARY_TIME = stringPreferencesKey("morning_summary_time")
        val ON_DEVICE_VOICE_ONLY = booleanPreferencesKey("on_device_voice_only")
        val VOICE_DIALECT = stringPreferencesKey("voice_dialect")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            userName = preferences[PreferencesKeys.USER_NAME] ?: "",
            hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            defaultLeadTimeMinutes = preferences[PreferencesKeys.DEFAULT_LEAD_TIME_MINUTES] ?: 0,
            staggeredAlerts = preferences[PreferencesKeys.STAGGERED_ALERTS] ?: true,
            wakeWordEnabled = preferences[PreferencesKeys.WAKE_WORD_ENABLED] ?: false,
            morningSummaryEnabled = preferences[PreferencesKeys.MORNING_SUMMARY_ENABLED] ?: true,
            morningSummaryTime = preferences[PreferencesKeys.MORNING_SUMMARY_TIME] ?: "08:00",
            onDeviceVoiceOnly = preferences[PreferencesKeys.ON_DEVICE_VOICE_ONLY] ?: true,
            voiceDialect = preferences[PreferencesKeys.VOICE_DIALECT] ?: "es-SV"
        )
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun completeOnboarding(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setDefaultLeadTimeMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_LEAD_TIME_MINUTES] = minutes
        }
    }

    suspend fun setStaggeredAlerts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STAGGERED_ALERTS] = enabled
        }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAKE_WORD_ENABLED] = enabled
        }
    }

    suspend fun setMorningSummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MORNING_SUMMARY_ENABLED] = enabled
        }
    }

    suspend fun setMorningSummaryTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MORNING_SUMMARY_TIME] = time
        }
    }

    suspend fun setOnDeviceVoiceOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ON_DEVICE_VOICE_ONLY] = enabled
        }
    }

    suspend fun setVoiceDialect(dialect: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_DIALECT] = dialect
        }
    }
}
