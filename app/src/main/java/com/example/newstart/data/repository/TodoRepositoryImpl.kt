package com.example.newstart.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.newstart.data.local.dao.TodoDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import com.example.newstart.data.remote.ApiService
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.TodoRepository
import com.example.newstart.widget.HabitWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
    private val todoDao: TodoDao,
    @ApplicationContext private val context: Context
) : TodoRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getTodos(): Flow<List<Todo>> {
        val userId = authRepository.currentUserId ?: ""
        if (userId.isNotEmpty()) {
            repositoryScope.launch {
                syncTodosFromNetwork(userId)
            }
        }
        return todoDao.getAllTodos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodoById(id: String): Todo? {
        return todoDao.getTodoById(id)?.toDomain()
    }

    override suspend fun insertTodo(todo: Todo) {
        val userId = authRepository.currentUserId ?: ""
        val todoWithId = todo.copy(userId = userId)
        
        // Local first
        todoDao.insertTodo(todoWithId.toEntity(isSynced = false))
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        
        // Remote
        try {
            val savedTodo = apiService.saveTodo(todoWithId)
            todoDao.insertTodo(savedTodo.toEntity(isSynced = true))
        } catch (e: Exception) {
            // Ignored for sync fallback later
        }
    }

    override suspend fun updateTodo(todo: Todo) {
        todoDao.updateTodo(todo.toEntity(isSynced = false))
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        try {
            val savedTodo = apiService.updateTodo(todo.id, todo)
            todoDao.updateTodo(savedTodo.toEntity(isSynced = true))
        } catch (e: Exception) {}
    }

    override suspend fun deleteTodo(todo: Todo) {
        todoDao.deleteTodo(todo.toEntity())
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        try {
            apiService.deleteTodo(todo.id)
        } catch (e: Exception) {}
    }

    override suspend fun toggleTodoCompletion(id: String, isCompleted: Boolean) {
        todoDao.toggleTodoCompletion(id, isCompleted)
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        try {
            apiService.toggleTodoCompletion(id, mapOf("isCompleted" to isCompleted))
        } catch (e: Exception) {}
    }

    private suspend fun syncTodosFromNetwork(userId: String) {
        try {
            val remoteTodos = apiService.getTodos(userId)
            if (remoteTodos.isNotEmpty()) {
                for (todo in remoteTodos) {
                    todoDao.insertTodo(todo.toEntity(isSynced = true))
                }
                try { HabitWidget().updateAll(context) } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Sync failed: ${e.message}", e)
        }
    }
}

