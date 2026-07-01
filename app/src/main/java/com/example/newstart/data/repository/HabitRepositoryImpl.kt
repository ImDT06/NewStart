package com.example.newstart.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import com.example.newstart.data.remote.NewStartApiService
import com.example.newstart.data.remote.dto.HabitDto
import com.example.newstart.data.worker.SyncWorker
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val habitDao: HabitDao,
    private val apiService: NewStartApiService,
    @ApplicationContext private val context: Context
) : HabitRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)

    private fun Habit.toDto() = HabitDto(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        reminderTime = reminderTime,
        isCompleted = isCompleted,
        date = date,
        userId = userId,
        squadId = squadId
    )

    private fun HabitDto.toDomain() = Habit(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        colorHex = colorHex ?: "#1D5FE2",
        isCompleted = isCompleted,
        date = date,
        reminderTime = reminderTime,
        squadId = squadId
    )

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniqueWork(
            "habit_sync_work",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override suspend fun saveHabit(habit: Habit): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            
            if (habit.squadId != null && habit.id.isEmpty()) {
                val squadDoc = firestore.collection("squads").document(habit.squadId).get().await()
                val members = squadDoc.get("members") as? List<String> ?: emptyList()
                
                for (memberId in members) {
                    val docRef = firestore.collection("habits").document()
                    val memberHabit = habit.copy(id = docRef.id, userId = memberId)
                    
                    if (memberId == userId) {
                        habitDao.insertHabit(memberHabit.toEntity(isSynced = false))
                    }
                    
                    try {
                        docRef.set(memberHabit).await()
                        if (memberId == userId) {
                            habitDao.insertHabit(memberHabit.toEntity(isSynced = true))
                            HabitReminderManager.scheduleReminder(context, memberHabit)
                        }
                    } catch (e: Exception) {
                        if (memberId == userId) {
                            scheduleSync()
                        }
                    }
                }
            } else {
                val docRef = if (habit.id.isEmpty()) {
                    firestore.collection("habits").document()
                } else {
                    firestore.collection("habits").document(habit.id)
                }
                
                val habitWithUserId = habit.copy(id = docRef.id, userId = userId)
                
                habitDao.insertHabit(habitWithUserId.toEntity(isSynced = false))
                
                // Save to Spring Boot Server
                try {
                    apiService.createHabit(habitWithUserId.toDto())
                } catch (e: Exception) {
                    android.util.Log.e("HabitRepository", "Spring Boot save failed: ${e.message}", e)
                }

                try {
                    docRef.set(habitWithUserId).await()
                    habitDao.insertHabit(habitWithUserId.toEntity(isSynced = true))
                } catch (e: Exception) {
                    scheduleSync()
                }
                HabitReminderManager.scheduleReminder(context, habitWithUserId)
            }
            
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
                // Sync xóa có thể phức tạp hơn, tạm thời chỉ xử lý thêm/sửa
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
                scheduleSync()
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
        
        // Kích hoạt việc fetch từ SERVER SPRING BOOT ngầm
        if (userId.isNotEmpty()) {
            repositoryScope.launch {
                try {
                    println(">>> Đang gọi API lấy dữ liệu từ Spring Boot...")
                    val remoteHabits = apiService.getHabits(date)
                    println(">>> Đã nhận được ${remoteHabits.size} thói quen từ Server!")
                    if (remoteHabits.isNotEmpty()) {
                        val currentLocalHabits = habitDao.getHabitsSync(userId, date)
                        val entitiesToInsert = remoteHabits.map { dto ->
                            val domain = dto.toDomain()
                            
                            // Kiểm tra xem có thói quen nào ở local trùng lặp (cùng tên, icon, ngày) nhưng khác ID không
                            val duplicate = currentLocalHabits.find { h ->
                                h.name == domain.name && 
                                h.icon == domain.icon && 
                                h.date == domain.date && 
                                h.id != domain.id
                            }
                            
                            if (duplicate != null) {
                                // Nếu tìm thấy trùng lặp, xóa cái cũ ở local đi để dùng cái từ server (có ID server)
                                habitDao.deleteHabit(duplicate.id)
                            }
                            
                            domain.toEntity(isSynced = true)
                        }
                        habitDao.insertHabits(entitiesToInsert)
                    }
                } catch (e: Exception) {
                    println(">>> Lỗi gọi API Spring Boot: ${e.message}")
                }
                syncHabitsFromNetwork(userId)
            }
        }

        return habitDao.getHabits(userId, date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllHabits(): Flow<List<Habit>> {
        val userId = auth.currentUser?.uid ?: ""
        return habitDao.getAllHabits(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private suspend fun syncHabitsFromNetwork(userId: String) {
        try {
            // Fetch all habits for this user (includes both personal and their copy of squad habits)
            val snapshot = firestore.collection("habits")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val remoteHabits = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Habit::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("HabitRepository", "Error deserializing habit: ${e.message}", e)
                    null
                }
            }

            if (remoteHabits.isNotEmpty()) {
                habitDao.insertHabits(remoteHabits.map { it.toEntity(isSynced = true) })
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Sync failed: ${e.message}", e)
        }
    }
}
