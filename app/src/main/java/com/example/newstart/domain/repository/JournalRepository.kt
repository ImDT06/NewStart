package com.example.newstart.domain.repository

import android.net.Uri
import com.example.newstart.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    suspend fun saveJournalEntry(emoji: String, text: String, imageUri: Uri?): Result<Unit>
    suspend fun deleteJournalEntry(entryId: String): Result<Unit>
    fun getJournalEntries(): Flow<List<JournalEntry>>
}
