package com.example.newstart.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.local.toEntity
import com.example.newstart.data.remote.ApiService
import com.example.newstart.data.worker.SyncWorker
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.util.HabitReminderManager
import com.example.newstart.widget.HabitWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
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
            val userId = authRepository.currentUserId ?: throw Exception("User not logged in")
            
            // Luu vao Room voi isSynced = false truoc
            val habitWithUserId = if (habit.userId.isEmpty()) habit.copy(userId = userId) else habit
            habitDao.insertHabit(habitWithUserId.toEntity(isSynced = false))

            try {
                // Gui api len backend de luu
                val savedHabit = if (habit.id.isEmpty()) {
                    apiService.saveHabit(habitWithUserId)
                } else {
                    apiService.updateHabit(habit.id, habitWithUserId)
                }
                // Cap nhat lai Room voi isSynced = true
                habitDao.insertHabit(savedHabit.toEntity(isSynced = true))
                HabitReminderManager.scheduleReminder(context, savedHabit)
            } catch (e: Exception) {
                // Neu loi network, len lich sync tu dong qua WorkManager
                scheduleSync()
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
            // 1. Xoa o Room
            habitDao.deleteHabit(habitId)

            // 2. Xoa o backend
            try {
                apiService.deleteHabit(habitId)
            } catch (e: Exception) {
                scheduleSync()
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
            // 1. Cap nhat Room ngay lap tuc
            habitDao.updateCompletion(habit.id, isCompleted, isSynced = false)

            // 2. Cap nhat backend
            try {
                apiService.toggleHabitCompletion(habit.id, mapOf("isCompleted" to isCompleted))
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
        val userId = authRepository.currentUserId ?: ""
        
        // Kich hoat viec fetch tu network ngam
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
        val userId = authRepository.currentUserId ?: ""
        return habitDao.getAllHabits(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private suspend fun syncHabitsFromNetwork(userId: String, date: String) {
        try {
            val remoteHabits = apiService.getHabits(date, userId)
            if (remoteHabits.isNotEmpty()) {
                habitDao.insertHabits(remoteHabits.map { it.toEntity(isSynced = true) })
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitRepository", "Sync failed: ${e.message}")
        }
    }
}

