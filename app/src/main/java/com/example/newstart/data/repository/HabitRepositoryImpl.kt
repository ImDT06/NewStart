package com.example.newstart.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.util.HabitReminderManager
import com.example.newstart.widget.HabitWidget
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
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
            
            // Đặt báo thức nhắc nhở
            HabitReminderManager.scheduleReminder(context, habitWithUserId)
            
            // Cập nhật Widget ngay lập tức
            HabitWidget().updateAll(context)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> {
        return try {
            firestore.collection("habits").document(habitId).delete().await()
            HabitReminderManager.cancelReminder(context, habitId)
            
            // Cập nhật Widget khi xóa
            HabitWidget().updateAll(context)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleHabitCompletion(habit: Habit, isCompleted: Boolean): Result<Unit> {
        return try {
            firestore.collection("habits").document(habit.id)
                .update("isCompleted", isCompleted).await()
            
            if (isCompleted) {
                // Nếu đã hoàn thành -> Hủy nhắc nhở
                HabitReminderManager.cancelReminder(context, habit.id)
            } else {
                // Nếu bỏ chọn hoàn thành -> Đặt lại nhắc nhở nếu có cài giờ
                HabitReminderManager.scheduleReminder(context, habit.copy(isCompleted = false))
            }

            // Cập nhật Widget khi thay đổi trạng thái
            HabitWidget().updateAll(context)

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
