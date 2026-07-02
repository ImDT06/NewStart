package com.example.newstart.data.repository

import com.example.newstart.data.local.dao.TodoDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import com.example.newstart.data.remote.NewStartApiService
import com.example.newstart.data.remote.dto.TodoDto
import com.example.newstart.domain.model.Todo
import com.example.newstart.domain.model.Priority
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
    private val apiService: NewStartApiService,
    @ApplicationContext private val context: Context
) : TodoRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private fun updateWidget() {
        repositoryScope.launch {
            kotlinx.coroutines.delay(100)
            try {
                HabitWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("TodoRepository", "Error updating widget: ${e.message}")
            }
        }
    }

    private fun Todo.toDto() = TodoDto(
        id = id.ifEmpty { null },
        task = task,
        isCompleted = isCompleted,
        priority = priority.name,
        dueDate = dueDate?.time,
        createdAt = createdAt?.time,
        userId = userId.ifEmpty { null }
    )

    private fun TodoDto.toDomain() = Todo(
        id = id ?: "",
        userId = userId ?: "",
        task = task,
        isCompleted = isCompleted,
        priority = try { Priority.valueOf(priority) } catch (e: Exception) { Priority.MEDIUM },
        dueDate = dueDate?.let { java.util.Date(it) },
        createdAt = createdAt?.let { java.util.Date(it) }
    )

    private fun Todo.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "task" to task,
            "isCompleted" to isCompleted,
            "priority" to priority.name,
            "dueDate" to dueDate?.time,
            "createdAt" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp())
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTodo(): Todo? {
        return try {
            val id = id
            val userId = getString("userId") ?: ""
            val task = getString("task") ?: ""
            val isCompleted = getBoolean("isCompleted") ?: false
            val priorityStr = getString("priority") ?: "MEDIUM"
            val priority = try { Priority.valueOf(priorityStr) } catch (e: Exception) { Priority.MEDIUM }
            
            val dueDateVal = get("dueDate")
            val dueDate = when (dueDateVal) {
                is com.google.firebase.Timestamp -> dueDateVal.toDate()
                is Long -> java.util.Date(dueDateVal)
                else -> null
            }
            
            val createdAtTimestamp = getTimestamp("createdAt")
            val createdAt = createdAtTimestamp?.toDate()
            
            Todo(
                id = id,
                userId = userId,
                task = task,
                isCompleted = isCompleted,
                priority = priority,
                dueDate = dueDate,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "toTodo mapping error: ${e.message}", e)
            null
        }
    }

    override fun getTodos(): Flow<List<Todo>> {
        val userId = auth.currentUser?.uid ?: ""
        if (userId.isNotEmpty()) {
            repositoryScope.launch {
                // 1. Fetch from Spring Boot Server
                try {
                    val remoteTodos = apiService.getTodos()
                    println(">>> Đã lấy được ${remoteTodos.size} việc cần làm từ Server!")
                    if (remoteTodos.isNotEmpty()) {
                        for (todoDto in remoteTodos) {
                            todoDao.insertTodo(todoDto.toDomain().toEntity(isSynced = true))
                        }
                        updateWidget()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TodoRepository", "Lỗi lấy Todo từ Spring Boot: ${e.message}", e)
                }
                // 2. Fetch from Firebase Firestore
                syncTodosFromNetwork(userId)
            }
        }
        return todoDao.getAllTodos(userId).map { entities ->
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
        updateWidget()
        
        // Remote (Spring Boot)
        var springBootSynced = false
        try {
            apiService.createTodo(todoWithId.toDto())
            springBootSynced = true
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Spring Boot insert failed: ${e.message}", e)
        }

        // Remote (Firestore)
        var firestoreSynced = false
        try {
            docRef.set(todoWithId.toFirestoreMap()).await()
            firestoreSynced = true
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Firestore insert failed: ${e.message}", e)
        }

        if (springBootSynced || firestoreSynced) {
            todoDao.insertTodo(todoWithId.toEntity(isSynced = true))
        }
    }

    override suspend fun updateTodo(todo: Todo) {
        todoDao.updateTodo(todo.toEntity(isSynced = false))
        updateWidget()
        
        var springBootSynced = false
        try {
            apiService.updateTodo(todo.id, todo.toDto())
            springBootSynced = true
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Spring Boot update failed: ${e.message}", e)
        }

        var firestoreSynced = false
        try {
            firestore.collection("todos").document(todo.id).set(todo.toFirestoreMap()).await()
            firestoreSynced = true
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Firestore update failed: ${e.message}", e)
        }

        if (springBootSynced || firestoreSynced) {
            todoDao.updateTodo(todo.toEntity(isSynced = true))
        }
    }

    override suspend fun deleteTodo(todo: Todo) {
        todoDao.deleteTodo(todo.toEntity())
        updateWidget()
        
        try {
            apiService.deleteTodo(todo.id)
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Spring Boot delete failed: ${e.message}", e)
        }

        try {
            firestore.collection("todos").document(todo.id).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Firestore delete failed: ${e.message}", e)
        }
    }

    override suspend fun toggleTodoCompletion(id: String, isCompleted: Boolean) {
        todoDao.toggleTodoCompletion(id, isCompleted)
        updateWidget()
        
        val localTodo = todoDao.getTodoById(id)?.toDomain()
        localTodo?.let { todo ->
            val updatedTodo = todo.copy(isCompleted = isCompleted)
            try {
                apiService.updateTodo(id, updatedTodo.toDto())
            } catch (e: Exception) {
                android.util.Log.e("TodoRepository", "Spring Boot toggle failed: ${e.message}", e)
            }
        }

        try {
            firestore.collection("todos").document(id).update("isCompleted", isCompleted).await()
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Firestore toggle failed: ${e.message}", e)
        }
    }

    private suspend fun syncTodosFromNetwork(userId: String) {
        try {
            val snapshot = firestore.collection("todos")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val remoteTodos = snapshot.documents.mapNotNull { doc ->
                doc.toTodo()
            }
            
            if (remoteTodos.isNotEmpty()) {
                for (todo in remoteTodos) {
                    todoDao.insertTodo(todo.toEntity(isSynced = true))
                }
                updateWidget()
            }
        } catch (e: Exception) {
            android.util.Log.e("TodoRepository", "Sync failed: ${e.message}", e)
        }
    }
}
