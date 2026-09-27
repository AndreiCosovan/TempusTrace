package com.example.tempustrace.data

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

data class WorkStats(
    val totalTrackedDays: Int = 0,
    val averageDailyMinutes: Long = 0,
    val totalWeekMinutes: Long = 0,
    val totalMonthMinutes: Long = 0,
    val daysWorkedThisWeek: Int = 0,
    val daysWorkedThisMonth: Int = 0,
    val timeBalanceMinutes: Long = 0
)

object WorkTimeCalculator {

    const val MAX_BREAK_MINUTES = 120

    fun netMinutes(workDay: WorkDay, breaks: List<Break>): Long? {
        val end = workDay.endTime ?: return null
        val breakMinutes = breaks.sumOf { it.durationMinutes ?: 0 }
        return Duration.between(workDay.startTime, end).toMinutes() - breakMinutes
    }

    /** A normal day is the configured shift minus the configured breaks, not a flat 8 hours. */
    fun expectedNetMinutes(preferences: UserPreferences): Long {
        val start = runCatching { LocalTime.parse(preferences.defaultWorkStartTime) }.getOrNull() ?: return 0
        val end = runCatching { LocalTime.parse(preferences.defaultWorkEndTime) }.getOrNull() ?: return 0
        val gross = Duration.between(start, end).toMinutes()
        if (gross <= 0) return 0
        val breakMinutes = preferences.defaultFirstBreakDuration.toLong() +
            preferences.defaultSecondBreakDuration.toLong()
        return (gross - breakMinutes).coerceAtLeast(0)
    }

    /** @return an error message, or null when the entry can be saved. */
    fun validateEntry(
        start: LocalTime,
        end: LocalTime,
        firstBreakMinutes: Int,
        secondBreakMinutes: Int
    ): String? {
        if (!end.isAfter(start)) {
            return "End time must be after start time."
        }
        if (firstBreakMinutes < 0 || secondBreakMinutes < 0) {
            return "Break durations cannot be negative."
        }
        if (firstBreakMinutes > MAX_BREAK_MINUTES || secondBreakMinutes > MAX_BREAK_MINUTES) {
            return "Breaks cannot be longer than $MAX_BREAK_MINUTES minutes."
        }
        val worked = Duration.between(start, end).toMinutes()
        if (firstBreakMinutes + secondBreakMinutes > worked) {
            return "Breaks are longer than the work period."
        }
        return null
    }

    fun stats(
        entries: List<WorkDayWithBreaks>,
        preferences: UserPreferences,
        today: LocalDate,
        weekStart: DayOfWeek
    ): WorkStats {
        val completed = entries.mapNotNull { entry ->
            netMinutes(entry.workDay, entry.breaks)?.let { minutes -> entry to minutes }
        }
        val weekStartDate = today.with(TemporalAdjusters.previousOrSame(weekStart))
        val weekEndDate = weekStartDate.plusDays(6)
        val thisWeek = completed.filter { (entry, _) ->
            val date = entry.workDay.date
            !date.isBefore(weekStartDate) && !date.isAfter(weekEndDate)
        }
        val thisMonth = completed.filter { (entry, _) ->
            val date = entry.workDay.date
            date.year == today.year && date.month == today.month
        }
        val totalMinutes = completed.sumOf { it.second }
        val days = completed.size
        val expected = expectedNetMinutes(preferences)
        return WorkStats(
            totalTrackedDays = days,
            averageDailyMinutes = if (days == 0) 0 else totalMinutes / days,
            totalWeekMinutes = thisWeek.sumOf { it.second },
            totalMonthMinutes = thisMonth.sumOf { it.second },
            daysWorkedThisWeek = thisWeek.size,
            daysWorkedThisMonth = thisMonth.size,
            timeBalanceMinutes = totalMinutes - days * expected
        )
    }

    /** MaterialDatePicker returns UTC midnight. Format that instant in UTC so the calendar day does not shift. */
    fun localDateFromPicker(selectionMillis: Long): LocalDate {
        return Instant.ofEpochMilli(selectionMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    fun pickerSelectionMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    fun formatHoursAndMinutes(minutes: Long): String {
        val sign = if (minutes < 0) "-" else ""
        val absolute = abs(minutes)
        return "%s%dh %02dm".format(Locale.getDefault(), sign, absolute / 60, absolute % 60)
    }

    fun formatSignedHoursAndMinutes(minutes: Long): String {
        val prefix = if (minutes > 0) "+" else ""
        return prefix + formatHoursAndMinutes(minutes)
    }
}
