package com.example.newstart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newstart.domain.model.Priority
import com.example.newstart.domain.model.Todo
import java.util.Date

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val task: String,
    val isCompleted: Boolean,
    val priority: String, // Lưu Enum dưới dạng String
    val dueDate: Long?,
    val createdAt: Long,
    val isSynced: Boolean = true
)

fun TodoEntity.toDomain() = Todo(
    id = id,
    userId = userId,
    task = task,
    isCompleted = isCompleted,
    priority = Priority.valueOf(priority),
    dueDate = dueDate?.let { Date(it) },
    createdAt = Date(createdAt)
)

fun Todo.toEntity(isSynced: Boolean = true) = TodoEntity(
    id = id,
    userId = userId,
    task = task,
    isCompleted = isCompleted,
    priority = priority.name,
    dueDate = dueDate?.time,
    createdAt = createdAt?.time ?: System.currentTimeMillis(),
    isSynced = isSynced
)
