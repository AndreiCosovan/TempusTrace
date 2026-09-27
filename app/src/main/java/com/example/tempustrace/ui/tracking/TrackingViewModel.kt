package com.example.tempustrace.ui.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.tempustrace.data.AppDatabase
import com.example.tempustrace.data.Break
import com.example.tempustrace.data.WorkDay
import com.example.tempustrace.data.WorkTimeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _saveResult = MutableLiveData<SaveResult?>()
    val saveResult: LiveData<SaveResult?> = _saveResult

    fun saveWorkDay(
        date: LocalDate,
        start: LocalTime,
        end: LocalTime,
        firstBreakMinutes: Int,
        secondBreakMinutes: Int
    ) {
        viewModelScope.launch {
            try {
                val validationError = WorkTimeCalculator.validateEntry(
                    start,
                    end,
                    firstBreakMinutes,
                    secondBreakMinutes
                )
                if (validationError != null) {
                    _saveResult.value = SaveResult.Error(validationError)
                    return@launch
                }

                val saved = database.withTransaction {
                    if (database.workDayDao().hasWorkDayForDate(date)) {
                        false
                    } else {
                        val workDayId = database.workDayDao().insertWorkDay(
                            WorkDay(date = date, startTime = start, endTime = end)
                        )
                        listOf(120L to firstBreakMinutes, 240L to secondBreakMinutes)
                            .filter { (_, minutes) -> minutes > 0 }
                            .forEach { (offsetMinutes, minutes) ->
                                val breakStart = start.plusMinutes(offsetMinutes)
                                database.breakDao().insertBreak(
                                    Break(
                                        workDayId = workDayId,
                                        startTime = breakStart,
                                        endTime = breakStart.plusMinutes(minutes.toLong()),
                                        durationMinutes = minutes
                                    )
                                )
                            }
                        true
                    }
                }
                _saveResult.value = if (saved) {
                    SaveResult.Success
                } else {
                    SaveResult.Error("An entry for this date already exists.")
                }
            } catch (exception: Exception) {
                _saveResult.value = SaveResult.Error(
                    exception.localizedMessage ?: "Could not save this workday."
                )
            }
        }
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }

    sealed interface SaveResult {
        data object Success : SaveResult
        data class Error(val message: String) : SaveResult
    }
}
