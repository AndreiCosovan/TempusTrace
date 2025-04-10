package com.example.tempustrace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "work_days")
data class WorkDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime? = null
)