package com.example.tempustrace.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tempustrace.data.AppDatabase
import com.example.tempustrace.data.Break
import com.example.tempustrace.data.WorkDay
import com.example.tempustrace.data.WorkDayWithBreaks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.compareTo
import kotlin.div
import kotlin.text.toDouble
import kotlin.text.toLong

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _workStats = MutableLiveData<WorkStats>()
    val workStats: LiveData<WorkStats> = _workStats

    private val _recentWorkDays = MutableLiveData<List<WorkDayWithBreaks>>()
    val recentWorkDays: LiveData<List<WorkDayWithBreaks>> = _recentWorkDays

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        loadWorkData()
    }

    fun loadWorkData() {
        _loading.value = true
        viewModelScope.launch {
            database.workDayDao().getAllWorkDays().collect { workDays ->
                if (workDays.isNotEmpty()) {
                    calculateStats(workDays)
                    loadRecentWorkDays()
                } else {
                    _workStats.value = WorkStats()
                    _recentWorkDays.value = emptyList()
                }
                _loading.value = false
            }
        }
    }

    private fun loadRecentWorkDays() {
        viewModelScope.launch {
            try {
                // Get all work days once with first()
                val workDays = database.workDayDao().getAllWorkDays().first()

                // Extract the 5 most recent IDs
                val recentWorkDayIds = workDays.sortedByDescending { it.date }
                    .take(5)
                    .map { it.id }

                // Collect each work day with its breaks
                val recentEntries = mutableListOf<WorkDayWithBreaks>()
                for (id in recentWorkDayIds) {
                    val workDayWithBreaks = database.workDayDao().getWorkDayWithBreaks(id).first()
                    recentEntries.add(workDayWithBreaks)
                }

                // Update the LiveData with the results
                _recentWorkDays.value = recentEntries
            } catch (e: Exception) {
                _recentWorkDays.value = emptyList()
            }
        }
    }

    private fun calculateStats(workDays: List<WorkDay>) {
        viewModelScope.launch {
            val now = LocalDate.now()
            // Get all workdays with their breaks
            val workDaysWithBreaks = mutableListOf<WorkDayWithBreaks>()
            for (workDay in workDays) {
                try {
                    val workDayWithBreaks = database.workDayDao().getWorkDayWithBreaks(workDay.id).first()
                    workDaysWithBreaks.add(workDayWithBreaks)
                } catch (e: Exception) {
                    // If breaks can't be loaded, just use the workday without breaks
                    workDaysWithBreaks.add(WorkDayWithBreaks(workDay, emptyList()))
                }
            }

            // Filter for weekly and monthly stats
            val thisWeekWorkDays = workDaysWithBreaks.filter {
                ChronoUnit.DAYS.between(it.workDay.date, now) < 7
            }

            val thisMonthWorkDays = workDaysWithBreaks.filter {
                it.workDay.date.month == now.month && it.workDay.date.year == now.year
            }

            // Calculate total actual hours worked (excluding breaks)
            val totalActualHours = workDaysWithBreaks.sumOf { workDayWithBreaks ->
                val workDay = workDayWithBreaks.workDay
                workDay.endTime?.let { endTime ->
                    // Calculate total work duration
                    val totalMinutes = Duration.between(workDay.startTime, endTime).toMinutes().toDouble()

                    // Subtract break durations
                    val breakMinutes = workDayWithBreaks.breaks.sumOf { it.durationMinutes?.toLong() ?: 0L }.toDouble()

                    // Convert to hours
                    (totalMinutes - breakMinutes) / 60.0
                } ?: 0.0
            }

            // Calculate total hours this week (excluding breaks)
            val totalWeekHours = thisWeekWorkDays.sumOf { workDayWithBreaks ->
                val workDay = workDayWithBreaks.workDay
                workDay.endTime?.let { endTime ->
                    val totalMinutes = Duration.between(workDay.startTime, endTime).toMinutes().toDouble()
                    val breakMinutes = workDayWithBreaks.breaks.sumOf { it.durationMinutes?.toLong() ?: 0L }.toDouble()
                    (totalMinutes - breakMinutes) / 60.0
                } ?: 0.0
            }

            // Calculate total hours this month (excluding breaks)
            val totalMonthHours = thisMonthWorkDays.sumOf { workDayWithBreaks ->
                val workDay = workDayWithBreaks.workDay
                workDay.endTime?.let { endTime ->
                    val totalMinutes = Duration.between(workDay.startTime, endTime).toMinutes().toDouble()
                    val breakMinutes = workDayWithBreaks.breaks.sumOf { it.durationMinutes?.toLong() ?: 0L }.toDouble()
                    (totalMinutes - breakMinutes) / 60.0
                } ?: 0.0
            }

            // Calculate time balance (actual worked hours - standard hours)
            val totalStandardHours = workDays.size * 8.0 // 8 hours per day is standard
            val timeBalance = totalActualHours - totalStandardHours

            // Calculate average daily hours
            val averageDailyHours = if (workDays.isNotEmpty()) {
                totalActualHours / workDays.size
            } else {
                0.0
            }

            _workStats.value = WorkStats(
                totalTrackedDays = workDays.size,
                averageDailyHours = averageDailyHours,
                totalWeekHours = totalWeekHours,
                totalMonthHours = totalMonthHours,
                daysWorkedThisWeek = thisWeekWorkDays.size,
                daysWorkedThisMonth = thisMonthWorkDays.size,
                timeBalance = timeBalance
            )
        }
    }

    fun deleteWorkDay(workDayId: Long) {
        viewModelScope.launch {
            try {
                // Delete the workday
                database.workDayDao().deleteWorkDayById(workDayId)
                // Reload data to update UI
                loadWorkData()
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    data class WorkStats(
        val totalTrackedDays: Int = 0,
        val averageDailyHours: Double = 0.0,
        val totalWeekHours: Double = 0.0,
        val totalMonthHours: Double = 0.0,
        val daysWorkedThisWeek: Int = 0,
        val daysWorkedThisMonth: Int = 0,
        val timeBalance: Double = 0.0
    )
}