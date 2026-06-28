package com.example.newstart.data.local.dao

import androidx.room.*
import com.example.newstart.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    suspend fun getAllTodosSync(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Query("UPDATE todos SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun toggleTodoCompletion(id: String, isCompleted: Boolean)
}
