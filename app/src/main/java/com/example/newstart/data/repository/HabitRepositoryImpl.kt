package com.example.newstart.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.util.HabitReminderManager
import com.example.newstart.widget.HabitWidget
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val habitDao: HabitDao,
    @ApplicationContext private val context: Context
) : HabitRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override suspend fun saveHabit(habit: Habit): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val docRef = if (habit.id.isEmpty()) {
                firestore.collection("habits").document()
            } else {
                firestore.collection("habits").document(habit.id)
            }
            
            val habitWithUserId = habit.copy(id = docRef.id, userId = userId)
            
            // 1. Lưu vào Room trước (Offline-first)
            habitDao.insertHabit(habitWithUserId.toEntity(isSynced = false))

            // 2. Lưu vào Firestore
            try {
                docRef.set(habitWithUserId).await()
                habitDao.insertHabit(habitWithUserId.toEntity(isSynced = true))
            } catch (e: Exception) {
                // Không sao, sẽ sync lại sau
            }
            
            // Đặt báo thức nhắc nhở
            HabitReminderManager.scheduleReminder(context, habitWithUserId)
            
            // Cập nhật Widget
            HabitWidget().updateAll(context)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> {
        return try {
            // 1. Xóa ở Room
            habitDao.deleteHabit(habitId)

            // 2. Xóa ở Firestore
            try {
                firestore.collection("habits").document(habitId).delete().await()
            } catch (e: Exception) {
                // Sẽ xử lý sync xóa sau
            }

            HabitReminderManager.cancelReminder(context, habitId)
            HabitWidget().updateAll(context)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleHabitCompletion(habit: Habit, isCompleted: Boolean): Result<Unit> {
        return try {
            // 1. Cập nhật Room ngay lập tức
            habitDao.updateCompletion(habit.id, isCompleted, isSynced = false)

            // 2. Cập nhật Firestore
            try {
                firestore.collection("habits").document(habit.id)
                    .update("isCompleted", isCompleted).await()
                habitDao.updateCompletion(habit.id, isCompleted, isSynced = true)
            } catch (e: Exception) {
                // Sync sau
            }
            
            if (isCompleted) {
                HabitReminderManager.cancelReminder(context, habit.id)
            } else {
                HabitReminderManager.scheduleReminder(context, habit.copy(isCompleted = false))
            }

            HabitWidget().updateAll(context)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getHabits(date: String): Flow<List<Habit>> {
        val userId = auth.currentUser?.uid ?: ""
        
        // Kích hoạt việc fetch từ network ngầm
        if (userId.isNotEmpty()) {
            repositoryScope.launch {
                syncHabitsFromNetwork(userId, date)
            }
        }

        return habitDao.getHabits(userId, date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private suspend fun syncHabitsFromNetwork(userId: String, date: String) {
        try {
            val snapshot = firestore.collection("habits")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .get()
                .await()
            
            val remoteHabits = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Habit::class.java)?.copy(id = doc.id)
            }

            if (remoteHabits.isNotEmpty()) {
                habitDao.insertHabits(remoteHabits.map { it.toEntity(isSynced = true) })
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Sync failed: ${e.message}")
        }
    }
}
