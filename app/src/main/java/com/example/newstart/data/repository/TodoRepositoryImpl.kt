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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val todoDao: TodoDao
) : TodoRepository {

    override fun getTodos(): Flow<List<Todo>> {
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
        try {
            firestore.collection("todos").document(todo.id).set(todo).await()
            todoDao.updateTodo(todo.toEntity(isSynced = true))
        } catch (e: Exception) {}
    }

    override suspend fun deleteTodo(todo: Todo) {
        todoDao.deleteTodo(todo.toEntity())
        try {
            firestore.collection("todos").document(todo.id).delete().await()
        } catch (e: Exception) {}
    }

    override suspend fun toggleTodoCompletion(id: String, isCompleted: Boolean) {
        todoDao.toggleTodoCompletion(id, isCompleted)
        try {
            firestore.collection("todos").document(id).update("isCompleted", isCompleted).await()
        } catch (e: Exception) {}
    }
}
