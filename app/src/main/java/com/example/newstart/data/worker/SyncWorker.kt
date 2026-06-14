package com.example.newstart.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.toDomain
import com.example.newstart.domain.model.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitDao: HabitDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()
        
        return try {
            // 1. Lấy tất cả thói quen chưa đồng bộ
            val unsyncedHabits = habitDao.getUnsyncedHabits()
            Log.d("SyncWorker", "Found ${unsyncedHabits.size} unsynced habits")

            for (habitEntity in unsyncedHabits) {
                val habit = habitEntity.toDomain()
                
                // 2. Đẩy lên Firestore
                firestore.collection("habits")
                    .document(habit.id)
                    .set(habit)
                    .await()
                
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
