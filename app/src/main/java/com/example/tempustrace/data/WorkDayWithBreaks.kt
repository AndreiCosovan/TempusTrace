package com.example.tempustrace.data

import androidx.room.Embedded
import androidx.room.Relation

data class WorkDayWithBreaks(
    @Embedded val workDay: WorkDay,
    @Relation(
        parentColumn = "id",
        entityColumn = "workDayId"
    )
    val breaks: List<Break>
)