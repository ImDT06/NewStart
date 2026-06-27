package com.example.newstart.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
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
    @ApplicationContext private val context: Context
) : HabitRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)

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

    override fun getAllHabits(): Flow<List<Habit>> {
        val userId = auth.currentUser?.uid ?: ""
        return habitDao.getAllHabits(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private suspend fun syncHabitsFromNetwork(userId: String, date: String) {
        try {
            // 1. Fetch personal habits
            val personalSnapshot = firestore.collection("habits")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .get()
                .await()
            
            val personalHabits = personalSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Habit::class.java)?.copy(id = doc.id)
            }

            // 2. Fetch squad habits (shared)
            val squadSnapshot = firestore.collection("habits")
                .whereEqualTo("date", date)
                .whereNotEqualTo("squadId", null)
                .get()
                .await()
            
            // Lọc lại những squad mà user tham gia (Firestore query whereIn có giới hạn, nên có thể lọc client-side nếu số lượng ít)
            val userSquads = firestore.collection("squads")
                .whereArrayContains("members", userId)
                .get()
                .await()
                .documents.map { it.id }

            val squadHabits = squadSnapshot.documents.mapNotNull { doc ->
                val habit = doc.toObject(Habit::class.java)?.copy(id = doc.id)
                if (habit?.squadId != null && userSquads.contains(habit.squadId)) habit else null
            }

            val allRemoteHabits = personalHabits + squadHabits

            if (allRemoteHabits.isNotEmpty()) {
                habitDao.insertHabits(allRemoteHabits.map { it.toEntity(isSynced = true) })
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Sync failed: ${e.message}")
        }
    }
}
