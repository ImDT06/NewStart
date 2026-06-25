package com.example.newstart.domain.repository

import android.net.Uri
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.model.MovieDetails
import com.example.newstart.domain.model.BookDetails
import com.example.newstart.domain.model.SubjectDetails
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    suspend fun saveJournalEntry(
        emoji: String,
        text: String,
        imageUri: Uri?,
        imageSource: String? = null,
        type: JournalType = JournalType.NORMAL,
        movieDetails: MovieDetails? = null,
        bookDetails: BookDetails? = null,
        subjectDetails: SubjectDetails? = null
    ): Result<Unit>
    suspend fun deleteJournalEntry(entryId: String): Result<Unit>
    fun getJournalEntries(): Flow<List<JournalEntry>>
}
