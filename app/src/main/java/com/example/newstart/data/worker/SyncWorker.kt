package com.example.newstart.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.data.remote.ApiService
import com.example.newstart.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitDao: HabitDao,
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = authRepository.currentUserId ?: return Result.failure()
        
        return try {
            // 1. Lấy tất cả thói quen chưa đồng bộ
            val unsyncedHabits = habitDao.getUnsyncedHabits()
            Log.d("SyncWorker", "Found ${unsyncedHabits.size} unsynced habits")

            for (habitEntity in unsyncedHabits) {
                val habit = habitEntity.toDomain()
                
                // 2. Đẩy lên backend API
                if (habit.id.isEmpty()) {
                    apiService.saveHabit(habit)
                } else {
                    apiService.updateHabit(habit.id, habit)
                }
                
                // 3. Đánh dấu đã đồng bộ trong Room
                habitDao.updateCompletion(
                    habitId = habit.id,
                    isCompleted = habit.isCompleted,
                    lastUpdated = habitEntity.lastUpdated,
                    isSynced = true
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}

