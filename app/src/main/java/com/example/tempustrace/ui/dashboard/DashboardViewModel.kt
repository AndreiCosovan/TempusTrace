package com.example.tempustrace.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.tempustrace.data.AppDatabase
import com.example.tempustrace.data.UserPreferencesRepository
import com.example.tempustrace.data.WorkDayWithBreaks
import com.example.tempustrace.data.WorkStats
import com.example.tempustrace.data.WorkTimeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val database: AppDatabase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _workStats = MutableLiveData(WorkStats())
    val workStats: LiveData<WorkStats> = _workStats

    private val _recentWorkDays = MutableLiveData<List<WorkDayWithBreaks>>(emptyList())
    val recentWorkDays: LiveData<List<WorkDayWithBreaks>> = _recentWorkDays

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    init {
        viewModelScope.launch {
            combine(
                database.workDayDao().getAllWorkDaysWithBreaks(),
                userPreferencesRepository.userPreferencesFlow
            ) { days, preferences ->
                val weekStart = WeekFields.of(Locale.getDefault()).firstDayOfWeek
                days to WorkTimeCalculator.stats(days, preferences, LocalDate.now(), weekStart)
            }.collect { (days, stats) ->
                _recentWorkDays.value = days
                _workStats.value = stats
                _loading.value = false
            }
        }
    }

    fun deleteWorkDay(workDayId: Long) {
        viewModelScope.launch {
            database.workDayDao().deleteWorkDayById(workDayId)
        }
    }

    fun restoreWorkDay(entry: WorkDayWithBreaks) {
        viewModelScope.launch {
            database.withTransaction {
                if (database.workDayDao().hasWorkDayForDate(entry.workDay.date)) {
                    return@withTransaction
                }
                val newId = database.workDayDao().insertWorkDay(entry.workDay.copy(id = 0))
                entry.breaks.forEach { breakEntry ->
                    database.breakDao().insertBreak(breakEntry.copy(id = 0, workDayId = newId))
                }
            }
        }
    }
}
