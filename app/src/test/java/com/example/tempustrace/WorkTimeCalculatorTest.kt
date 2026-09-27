package com.example.tempustrace

import com.example.tempustrace.data.Break
import com.example.tempustrace.data.UserPreferences
import com.example.tempustrace.data.WorkDay
import com.example.tempustrace.data.WorkDayWithBreaks
import com.example.tempustrace.data.WorkTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.TimeZone

class WorkTimeCalculatorTest {

    private val preferences = UserPreferences(
        defaultWorkStartTime = "09:00",
        defaultWorkEndTime = "17:00",
        defaultFirstBreakDuration = 18,
        defaultSecondBreakDuration = 36
    )

    @Test
    fun fullConfiguredDayHasZeroBalance() {
        val wednesday = LocalDate.of(2026, 9, 23)
        val stats = WorkTimeCalculator.stats(
            listOf(entry(wednesday, LocalTime.of(9, 0), LocalTime.of(17, 0), listOf(18, 36))),
            preferences,
            wednesday,
            DayOfWeek.MONDAY
        )

        assertEquals(426L, stats.averageDailyMinutes)
        assertEquals(0L, stats.timeBalanceMinutes)
    }

    @Test
    fun thisWeekIsTheCalendarWeek() {
        val wednesday = LocalDate.of(2026, 9, 23)
        val previousSunday = LocalDate.of(2026, 9, 20)
        val monday = LocalDate.of(2026, 9, 21)
        val stats = WorkTimeCalculator.stats(
            listOf(
                entry(previousSunday, LocalTime.of(9, 0), LocalTime.of(17, 0), emptyList()),
                entry(monday, LocalTime.of(9, 0), LocalTime.of(17, 0), emptyList()),
                entry(wednesday, LocalTime.of(9, 0), LocalTime.of(17, 0), emptyList())
            ),
            preferences,
            wednesday,
            DayOfWeek.MONDAY
        )

        assertEquals(2, stats.daysWorkedThisWeek)
        assertEquals(3, stats.daysWorkedThisMonth)
        assertEquals(16 * 60L, stats.totalWeekMinutes)
    }

    @Test
    fun endTimeMustBeAfterStartAndBreaksMustFit() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 0)

        assertEquals(
            "End time must be after start time.",
            WorkTimeCalculator.validateEntry(end, start, 18, 36)
        )
        assertEquals(
            "Breaks are longer than the work period.",
            WorkTimeCalculator.validateEntry(start, start.plusMinutes(30), 18, 36)
        )
        assertNull(WorkTimeCalculator.validateEntry(start, end, 18, 36))
    }

    @Test
    fun datePickerSelectionStaysOnTheUtcCalendarDay() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val date = LocalDate.of(2026, 9, 23)
            val utcMidnight = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            assertEquals(date, WorkTimeCalculator.localDateFromPicker(utcMidnight))
            assertEquals(utcMidnight, WorkTimeCalculator.pickerSelectionMillis(date))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    private fun entry(
        date: LocalDate,
        start: LocalTime,
        end: LocalTime,
        breakMinutes: List<Int>
    ): WorkDayWithBreaks {
        val workDay = WorkDay(id = 1, date = date, startTime = start, endTime = end)
        val breaks = breakMinutes.map { minutes ->
            Break(workDayId = workDay.id, startTime = start, durationMinutes = minutes)
        }
        return WorkDayWithBreaks(workDay, breaks)
    }
}
