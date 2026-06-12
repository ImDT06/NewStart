package com.example.newstart.domain.repository

import com.example.newstart.domain.model.Habit
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    suspend fun saveHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun toggleHabitCompletion(habit: Habit, isCompleted: Boolean): Result<Unit>
    fun getHabits(date: String): Flow<List<Habit>>
    fun getAllHabits(): Flow<List<Habit>>
}
