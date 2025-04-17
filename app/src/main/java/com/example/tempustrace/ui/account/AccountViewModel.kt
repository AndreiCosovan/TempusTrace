package com.example.tempustrace.ui.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tempustrace.data.UserPreferencesRepository
import com.example.tempustrace.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _preferences = MutableLiveData<UserPreferences>()
    val preferences: LiveData<UserPreferences> = _preferences

    private val _saveStatus = MutableLiveData<SaveResult?>()
    val saveStatus: LiveData<SaveResult?> = _saveStatus

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        viewModelScope.launch {
            val userPrefs = userPreferencesRepository.userPreferencesFlow.first()
            _preferences.value = userPrefs
        }
    }

    fun savePreferences(
        startTime: String,
        endTime: String,
        firstBreakDuration: Int,
        secondBreakDuration: Int
    ) {
        viewModelScope.launch {
            try {
                // Validate time format
                if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
                    _saveStatus.value = SaveResult(false, "Invalid time format. Use HH:MM format.")
                    return@launch
                }

                // Validate start time is before end time
                if (!isStartBeforeEnd(startTime, endTime)) {
                    _saveStatus.value = SaveResult(false, "Start time must be before end time.")
                    return@launch
                }

                // Validate break durations
                if (firstBreakDuration < 0 || secondBreakDuration < 0) {
                    _saveStatus.value = SaveResult(false, "Break durations cannot be negative.")
                    return@launch
                }

                // Save preferences
                userPreferencesRepository.updateWorkStartTime(startTime)
                userPreferencesRepository.updateWorkEndTime(endTime)
                userPreferencesRepository.updateFirstBreakDuration(firstBreakDuration)
                userPreferencesRepository.updateSecondBreakDuration(secondBreakDuration)

                _saveStatus.value = SaveResult(true, "Settings saved successfully")
                loadPreferences() // Reload preferences to ensure UI is updated
            } catch (e: Exception) {
                _saveStatus.value = SaveResult(false, "Failed to save settings: ${e.message}")
            }
        }
    }

    private fun isValidTimeFormat(timeString: String): Boolean {
        return try {
            LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    private fun isStartBeforeEnd(startTime: String, endTime: String): Boolean {
        val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
        val end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
        return start.isBefore(end)
    }

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    data class SaveResult(
        val success: Boolean,
        val message: String
    )
}