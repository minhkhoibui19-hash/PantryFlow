package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.pantryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pantryflow_preferences"
)

data class PantrySettings(
    val warningDaysThreshold: Int = 3,
    val reminderDaysBefore: Int = 2,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false
) {
    val formattedReminderTime: String
        get() = String.format(Locale.US, "%02d:%02d", reminderHour, reminderMinute)
}

class UserPreferences(private val context: Context) {
    private object Keys {
        val HAS_SEEDED_INITIAL_DATA = booleanPreferencesKey("has_seeded_initial_data")
        val WARNING_DAYS_THRESHOLD = intPreferencesKey("warning_days_threshold")
        val REMINDER_DAYS_BEFORE = intPreferencesKey("reminder_days_before")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    }

    val settingsFlow: Flow<PantrySettings> = context.pantryDataStore.data.map { prefs ->
        PantrySettings(
            warningDaysThreshold = prefs[Keys.WARNING_DAYS_THRESHOLD] ?: 3,
            reminderDaysBefore = prefs[Keys.REMINDER_DAYS_BEFORE] ?: 2,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 8,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            darkModeEnabled = prefs[Keys.DARK_MODE_ENABLED] ?: false
        )
    }

    suspend fun hasSeededInitialData(): Boolean {
        return context.pantryDataStore.data.first()[Keys.HAS_SEEDED_INITIAL_DATA] ?: false
    }

    suspend fun markInitialDataSeeded() {
        context.pantryDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED_INITIAL_DATA] = true
        }
    }

    suspend fun setWarningDaysThreshold(days: Int) {
        context.pantryDataStore.edit { prefs ->
            prefs[Keys.WARNING_DAYS_THRESHOLD] = days.coerceIn(2, 7)
        }
    }

    suspend fun setReminderDaysBefore(days: Int) {
        context.pantryDataStore.edit { prefs ->
            prefs[Keys.REMINDER_DAYS_BEFORE] = days.coerceIn(1, 3)
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.pantryDataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.pantryDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.pantryDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE_ENABLED] = enabled
        }
    }
}
