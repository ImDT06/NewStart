package com.example.newstart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newstart.domain.model.*
import java.util.Date

@Entity(tableName = "journals")
data class JournalEntryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val emoji: String,
    val text: String,
    val imageUrl: String?,
    val imageSource: String?,
    val linkedHabitId: String?,
    val linkedTodoId: String?,
    val privacy: String,
    val type: String,
    
    // Movie details
    val movieTitle: String?,
    val movieDirector: String?,
    val movieActors: String?,
    val movieRating: Float?,
    
    // Book details
    val bookTitle: String?,
    val bookAuthor: String?,
    val bookPagesRead: Int?,
    val bookRating: Float?,
    
    // Subject details
    val subjectName: String?,
    val subjectTopic: String?,
    val subjectScore: Float?,
    val subjectUnderstandingLevel: Int?,
    
    // Local metadata
    val timestamp: Long,
    val localImageUri: String?,
    val isSynced: Boolean = true
)

fun JournalEntryEntity.toDomain() = JournalEntry(
    id = id,
    userId = userId,
    emoji = emoji,
    text = text,
    imageUrl = imageUrl,
    imageSource = imageSource,
    linkedHabitId = linkedHabitId,
    linkedTodoId = linkedTodoId,
    privacy = JournalPrivacy.valueOf(privacy),
    type = JournalType.valueOf(type),
    movieDetails = if (movieTitle != null) MovieDetails(
        title = movieTitle,
        director = movieDirector ?: "",
        actors = movieActors ?: "",
        rating = movieRating ?: 0f
    ) else null,
    bookDetails = if (bookTitle != null) BookDetails(
        title = bookTitle,
        author = bookAuthor ?: "",
        pagesRead = bookPagesRead ?: 0,
        rating = bookRating ?: 0f
    ) else null,
    subjectDetails = if (subjectName != null) SubjectDetails(
        name = subjectName,
        topic = subjectTopic ?: "",
        score = subjectScore,
        understandingLevel = subjectUnderstandingLevel ?: 3
    ) else null,
    timestamp = Date(timestamp),
    reactions = emptyMap()
)

fun JournalEntry.toEntity(isSynced: Boolean = true, localImageUri: String? = null) = JournalEntryEntity(
    id = id,
    userId = userId,
    emoji = emoji,
    text = text,
    imageUrl = imageUrl,
    imageSource = imageSource,
    linkedHabitId = linkedHabitId,
    linkedTodoId = linkedTodoId,
    privacy = privacy.name,
    type = type.name,
    movieTitle = movieDetails?.title,
    movieDirector = movieDetails?.director,
    movieActors = movieDetails?.actors,
    movieRating = movieDetails?.rating,
    bookTitle = bookDetails?.title,
    bookAuthor = bookDetails?.author,
    bookPagesRead = bookDetails?.pagesRead,
    bookRating = bookDetails?.rating,
    subjectName = subjectDetails?.name,
    subjectTopic = subjectDetails?.topic,
    subjectScore = subjectDetails?.score,
    subjectUnderstandingLevel = subjectDetails?.understandingLevel,
    timestamp = timestamp?.time ?: System.currentTimeMillis(),
    localImageUri = localImageUri,
    isSynced = isSynced
)
