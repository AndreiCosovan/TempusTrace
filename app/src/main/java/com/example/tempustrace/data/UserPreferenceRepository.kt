package com.example.tempustrace.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property for Context that creates a single DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository that stores user preferences such as theme settings, work times, etc.
 */
class UserPreferencesRepository(private val context: Context) {

    // Define your preference keys
    companion object {
        val DEFAULT_WORK_START_TIME = stringPreferencesKey("default_work_start_time")
        val DEFAULT_WORK_END_TIME = stringPreferencesKey("default_work_end_time")
        val DEFAULT_FIRST_BREAK_DURATION = intPreferencesKey("default_first_break_duration")
        val DEFAULT_SECOND_BREAK_DURATION = intPreferencesKey("default_second_break_duration")
    }

    // Get all settings as a Flow
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                defaultWorkStartTime = preferences[DEFAULT_WORK_START_TIME] ?: "09:00",
                defaultWorkEndTime = preferences[DEFAULT_WORK_END_TIME] ?: "17:00",
                defaultFirstBreakDuration = preferences[DEFAULT_FIRST_BREAK_DURATION] ?: 18,
                defaultSecondBreakDuration = preferences[DEFAULT_SECOND_BREAK_DURATION] ?: 36
            )
        }

    // Update individual settings
    suspend fun updateWorkStartTime(startTime: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_WORK_START_TIME] = startTime
        }
    }

    suspend fun updateWorkEndTime(endTime: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_WORK_END_TIME] = endTime
        }
    }

    suspend fun updateFirstBreakDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_FIRST_BREAK_DURATION] = minutes
        }
    }

    suspend fun updateSecondBreakDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_SECOND_BREAK_DURATION] = minutes
        }
    }
}

// Data class representing user preferences
data class UserPreferences(
    val defaultWorkStartTime: String,
    val defaultWorkEndTime: String,
    val defaultFirstBreakDuration: Int,
    val defaultSecondBreakDuration: Int
)