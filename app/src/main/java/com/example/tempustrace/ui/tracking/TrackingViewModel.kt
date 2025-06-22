package com.example.tempustrace.ui.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tempustrace.data.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    suspend fun hasWorkDayForDate(date: LocalDate): Boolean {
        return database.workDayDao().hasWorkDayForDate(date)
    }
}