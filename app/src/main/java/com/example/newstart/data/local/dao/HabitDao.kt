package com.example.newstart.data.local.dao

import androidx.room.*
import com.example.newstart.data.local.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE (userId = :userId OR squadId IS NOT NULL) AND date = :date ORDER BY createdAt DESC")
    fun getHabits(userId: String, date: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId OR squadId IS NOT NULL ORDER BY date DESC")
    fun getAllHabits(userId: String): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String)

    @Query("UPDATE habits SET isCompleted = :isCompleted, lastUpdated = :lastUpdated, isSynced = :isSynced WHERE id = :habitId")
    suspend fun updateCompletion(habitId: String, isCompleted: Boolean, lastUpdated: Long = System.currentTimeMillis(), isSynced: Boolean)

    @Query("SELECT * FROM habits WHERE isSynced = 0")
    suspend fun getUnsyncedHabits(): List<HabitEntity>
    
    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}
