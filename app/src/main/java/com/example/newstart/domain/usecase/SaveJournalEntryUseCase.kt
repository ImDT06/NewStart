package com.example.newstart.domain.usecase

import android.net.Uri
import com.example.newstart.domain.model.*
import com.example.newstart.domain.repository.JournalRepository
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(
        emoji: String,
        text: String,
        imageUri: Uri?,
        imageSource: String? = null,
        type: JournalType = JournalType.NORMAL,
        movieDetails: MovieDetails? = null,
        bookDetails: BookDetails? = null,
        subjectDetails: SubjectDetails? = null,
        privacy: JournalPrivacy = JournalPrivacy.FRIENDS
    ): Result<Unit> {
        return journalRepository.saveJournalEntry(
            emoji, text, imageUri, imageSource, type, movieDetails, bookDetails, subjectDetails, privacy
        )
    }
}
