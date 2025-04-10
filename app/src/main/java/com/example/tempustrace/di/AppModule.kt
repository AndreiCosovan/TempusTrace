package com.example.tempustrace.di

import android.content.Context
import com.example.tempustrace.data.AppDatabase
import com.example.tempustrace.data.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }
}