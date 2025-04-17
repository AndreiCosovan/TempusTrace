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
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

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
            // Get the 5 most recent work days with their breaks
            val recentIds = database.workDayDao().getAllWorkDays().collect { workDays ->
                val recentWorkDayIds = workDays.sortedByDescending { it.date }
                    .take(5)
                    .map { it.id }

                val recentEntries = mutableListOf<WorkDayWithBreaks>()
                for (id in recentWorkDayIds) {
                    database.workDayDao().getWorkDayWithBreaks(id).collect {
                        recentEntries.add(it)
                    }
                }
                _recentWorkDays.value = recentEntries
            }
        }
    }

    private fun calculateStats(workDays: List<WorkDay>) {
        val now = LocalDate.now()
        val thisWeekWorkDays = workDays.filter {
            ChronoUnit.DAYS.between(it.date, now) < 7
        }

        val thisMonthWorkDays = workDays.filter {
            it.date.month == now.month && it.date.year == now.year
        }

        // Calculate average hours worked per day
        val avgDailyHours = workDays.mapNotNull { workDay ->
            workDay.endTime?.let { endTime ->
                Duration.between(workDay.startTime, endTime).toMinutes().toDouble() / 60
            }
        }.average().takeIf { it.isFinite() } ?: 0.0

        // Calculate total hours this week
        val totalWeekHours = thisWeekWorkDays.sumOf { workDay ->
            workDay.endTime?.let { endTime ->
                Duration.between(workDay.startTime, endTime).toMinutes()
            } ?: 0
        } / 60.0

        // Calculate total hours this month
        val totalMonthHours = thisMonthWorkDays.sumOf { workDay ->
            workDay.endTime?.let { endTime ->
                Duration.between(workDay.startTime, endTime).toMinutes()
            } ?: 0
        } / 60.0

        _workStats.value = WorkStats(
            totalTrackedDays = workDays.size,
            averageDailyHours = avgDailyHours,
            totalWeekHours = totalWeekHours,
            totalMonthHours = totalMonthHours,
            daysWorkedThisWeek = thisWeekWorkDays.size,
            daysWorkedThisMonth = thisMonthWorkDays.size
        )
    }

    data class WorkStats(
        val totalTrackedDays: Int = 0,
        val averageDailyHours: Double = 0.0,
        val totalWeekHours: Double = 0.0,
        val totalMonthHours: Double = 0.0,
        val daysWorkedThisWeek: Int = 0,
        val daysWorkedThisMonth: Int = 0
    )
}