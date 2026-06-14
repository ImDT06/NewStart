package com.example.newstart.di

import android.content.Context
import androidx.room.Room
import com.example.newstart.data.local.NewStartDatabase
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.dao.TodoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewStartDatabase {
        return Room.databaseBuilder(
            context,
            NewStartDatabase::class.java,
            NewStartDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideHabitDao(database: NewStartDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    fun provideTodoDao(database: NewStartDatabase): TodoDao {
        return database.todoDao()
    }
}
