package com.example.newstart.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newstart.domain.repository.JournalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class JournalSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val journalRepository: JournalRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("JournalSyncWorker", "Starting background journal sync...")
        return try {
            val result = journalRepository.syncUnsyncedJournals()
            if (result.isSuccess) {
                Log.d("JournalSyncWorker", "Journal sync completed successfully")
                Result.success()
            } else {
                val exception = result.exceptionOrNull()
                Log.e("JournalSyncWorker", "Journal sync failed: ${exception?.message}", exception)
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("JournalSyncWorker", "Journal sync unexpected error: ${e.message}", e)
            Result.retry()
        }
    }
}
