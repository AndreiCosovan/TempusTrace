package com.example.tempustrace.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(
    tableName = "breaks",
    foreignKeys = [
        ForeignKey(
            entity = WorkDay::class,
            parentColumns = ["id"],
            childColumns = ["workDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workDayId")]
)
data class Break(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workDayId: Long,
    val startTime: LocalTime,
    val endTime: LocalTime? = null,
    val durationMinutes: Int? = null, 
)

