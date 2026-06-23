package com.example.newstart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newstart.domain.model.Habit

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val progress: String,
    val goal: String,
    val streak: Int,
    val colorHex: String,
    val isCompleted: Boolean,
    val date: String,
    val reminderTime: String?,
    val reminderMinutesBefore: Int,
    val squadId: String?,
    val createdAt: Long,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    progress = progress,
    goal = goal,
    streak = streak,
    colorHex = colorHex,
    isCompleted = isCompleted,
    date = date,
    reminderTime = reminderTime,
    reminderMinutesBefore = reminderMinutesBefore,
    squadId = squadId
)

fun Habit.toEntity(isSynced: Boolean = true) = HabitEntity(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    progress = progress,
    goal = goal,
    streak = streak,
    colorHex = colorHex,
    isCompleted = isCompleted,
    date = date,
    reminderTime = reminderTime,
    reminderMinutesBefore = reminderMinutesBefore,
    squadId = squadId,
    createdAt = createdAt.time,
    isSynced = isSynced
)
