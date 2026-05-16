package com.example.newstart.data.repository

import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.HabitRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : HabitRepository {

    override suspend fun saveHabit(habit: Habit): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val docRef = if (habit.id.isEmpty()) {
                firestore.collection("habits").document()
            } else {
                firestore.collection("habits").document(habit.id)
            }
            
            val habitWithUserId = habit.copy(id = docRef.id, userId = userId)
            docRef.set(habitWithUserId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> {
        return try {
            firestore.collection("habits").document(habitId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleHabitCompletion(habitId: String, isCompleted: Boolean): Result<Unit> {
        return try {
            firestore.collection("habits").document(habitId)
                .update("isCompleted", isCompleted).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getHabits(date: String): Flow<List<Habit>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            return@callbackFlow
        }

        val subscription = firestore.collection("habits")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("HabitRepository", "Lỗi lấy dữ liệu: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val habits = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Habit::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("HabitRepository", "Lỗi chuyển đổi: ${e.message}")
                            null
                        }
                    }.sortedByDescending { it.createdAt }

                    trySend(habits)
                }
            }
        awaitClose { subscription.remove() }
    }
}
