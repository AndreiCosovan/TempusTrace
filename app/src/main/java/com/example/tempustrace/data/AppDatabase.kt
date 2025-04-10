package com.example.tempustrace.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tempustrace.data.WorkDay
import com.example.tempustrace.data.Break
import com.example.tempustrace.data.Converters
import com.example.tempustrace.data.WorkDayDao
import com.example.tempustrace.data.BreakDao

@Database(
    entities = [WorkDay::class, Break::class],
    version = 1, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao
    abstract fun breakDao(): BreakDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tempus-trace.db"
                )
                    .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}