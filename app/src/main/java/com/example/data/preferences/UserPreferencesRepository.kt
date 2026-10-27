package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val currentDeck: Int = 1,
    val dailyGoal: Int = 10,
    val streakDays: Int = 1,
    val lastActiveDate: String = "",
    val notificationsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val arabicFontSize: Float = 32f,
    val totalWordsLearned: Int = 0,
    val themeMode: String = "SYSTEM" // SYSTEM, LIGHT, DARK
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CURRENT_DECK = intPreferencesKey("current_deck")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val ARABIC_FONT_SIZE = floatPreferencesKey("arabic_font_size")
        val TOTAL_LEARNED = intPreferencesKey("total_words_learned")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            currentDeck = preferences[PreferencesKeys.CURRENT_DECK] ?: 1,
            dailyGoal = preferences[PreferencesKeys.DAILY_GOAL] ?: 10,
            streakDays = preferences[PreferencesKeys.STREAK_DAYS] ?: 1,
            lastActiveDate = preferences[PreferencesKeys.LAST_ACTIVE_DATE] ?: "",
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            soundEffectsEnabled = preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] ?: true,
            arabicFontSize = preferences[PreferencesKeys.ARABIC_FONT_SIZE] ?: 32f,
            totalWordsLearned = preferences[PreferencesKeys.TOTAL_LEARNED] ?: 0,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
        )
    }

    suspend fun updateCurrentDeck(deck: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_DECK] = deck
        }
    }

    suspend fun updateDailyGoal(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_GOAL] = goal
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateSoundEffects(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] = enabled
        }
    }

    suspend fun updateArabicFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ARABIC_FONT_SIZE] = size
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun recordActivityToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.dataStore.edit { preferences ->
            val lastDate = preferences[PreferencesKeys.LAST_ACTIVE_DATE] ?: ""
            var currentStreak = preferences[PreferencesKeys.STREAK_DAYS] ?: 1
            if (lastDate.isEmpty()) {
                preferences[PreferencesKeys.LAST_ACTIVE_DATE] = today
                preferences[PreferencesKeys.STREAK_DAYS] = 1
            } else if (lastDate != today) {
                // Check if yesterday
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 86400000L))
                if (lastDate == yesterday) {
                    currentStreak += 1
                } else {
                    currentStreak = 1
                }
                preferences[PreferencesKeys.LAST_ACTIVE_DATE] = today
                preferences[PreferencesKeys.STREAK_DAYS] = currentStreak
            }
        }
    }
}
