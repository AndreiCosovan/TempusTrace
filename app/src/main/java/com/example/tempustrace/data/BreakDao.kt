package com.example.tempustrace.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreak(breakEntity: Break): Long

    @Update
    suspend fun updateBreak(breakEntity: Break)

    @Delete
    suspend fun deleteBreak(breakEntity: Break)

    @Query("SELECT * FROM breaks WHERE workDayId = :workDayId ORDER BY startTime ASC")
    fun getBreaksForWorkDay(workDayId: Long): Flow<List<Break>>
}