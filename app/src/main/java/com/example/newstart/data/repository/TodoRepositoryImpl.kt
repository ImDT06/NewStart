package com.example.newstart.data.repository

import com.example.newstart.data.local.dao.TodoDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.repository.TodoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.newstart.widget.HabitWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val todoDao: TodoDao,
    @ApplicationContext private val context: Context
) : TodoRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getTodos(): Flow<List<Todo>> {
        val userId = auth.currentUser?.uid ?: ""
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
        val userId = auth.currentUser?.uid ?: ""
        val docRef = if (todo.id.isEmpty()) {
            firestore.collection("todos").document()
        } else {
            firestore.collection("todos").document(todo.id)
        }
        val todoWithId = todo.copy(id = docRef.id, userId = userId)
        
        // Local first
        todoDao.insertTodo(todoWithId.toEntity(isSynced = false))
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        
        // Remote
        try {
            docRef.set(todoWithId).await()
            todoDao.insertTodo(todoWithId.toEntity(isSynced = true))
        } catch (e: Exception) {
            // Error handling ignored for simplicity, reliance on sync logic later
        }
    }

    override suspend fun updateTodo(todo: Todo) {
        todoDao.updateTodo(todo.toEntity(isSynced = false))
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        try {
            firestore.collection("todos").document(todo.id).set(todo).await()
            todoDao.updateTodo(todo.toEntity(isSynced = true))
        } catch (e: Exception) {}
    }

    override suspend fun deleteTodo(todo: Todo) {
        todoDao.deleteTodo(todo.toEntity())
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        try {
            firestore.collection("todos").document(todo.id).delete().await()
        } catch (e: Exception) {}
    }

    override suspend fun toggleTodoCompletion(id: String, isCompleted: Boolean) {
        todoDao.toggleTodoCompletion(id, isCompleted)
        try { HabitWidget().updateAll(context) } catch (e: Exception) {}
        try {
            firestore.collection("todos").document(id).update("isCompleted", isCompleted).await()
        } catch (e: Exception) {}
    }

    private suspend fun syncTodosFromNetwork(userId: String) {
        try {
            val snapshot = firestore.collection("todos")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val remoteTodos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Todo::class.java)?.copy(id = doc.id)
            }
            
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
