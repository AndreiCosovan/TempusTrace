package com.example.tempustrace.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDay): Long

    @Update
    suspend fun updateWorkDay(workDay: WorkDay)

    @Delete
    suspend fun deleteWorkDay(workDay: WorkDay)

    @Query("SELECT * FROM work_days WHERE id = :id")
    suspend fun getWorkDayById(id: Long): WorkDay?

    @Query("SELECT * FROM work_days ORDER BY date DESC")
    fun getAllWorkDays(): Flow<List<WorkDay>>

    @Query("DELETE FROM work_days WHERE id = :workDayId")
    suspend fun deleteWorkDayById(workDayId: Long)

    @Transaction
    @Query("SELECT * FROM work_days WHERE id = :id")
    fun getWorkDayWithBreaks(id: Long): Flow<WorkDayWithBreaks>
}