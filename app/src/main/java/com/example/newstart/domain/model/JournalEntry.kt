package com.example.newstart.domain.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Immutable
data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val emoji: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val imageSource: String? = null, // "CAMERA" hoặc "GALLERY"
    val linkedHabitId: String? = null, // ID của thói quen liên kết
    val linkedTodoId: String? = null,  // ID của tác vụ liên kết
    val privacy: JournalPrivacy = JournalPrivacy.FRIENDS, // Quyền riêng tư
    val type: JournalType = JournalType.NORMAL,
    val movieDetails: MovieDetails? = null,
    val bookDetails: BookDetails? = null,
    val subjectDetails: SubjectDetails? = null,
    val reactions: Map<String, String> = emptyMap(), // Thả cảm xúc: userId -> emoji
    @ServerTimestamp
    val timestamp: Date? = null
)

enum class JournalType {
    NORMAL, MOVIE, BOOK, SUBJECT, RESTAURANT
}

data class MovieDetails(
    val title: String = "",
    val director: String = "",
    val actors: String = "",
    val rating: Float = 0f
)

data class BookDetails(
    val title: String = "",
    val author: String = "",
    val pagesRead: Int = 0,
    val rating: Float = 0f
)

data class SubjectDetails(
    val name: String = "",
    val topic: String = "",
    val score: Float? = null,
    val understandingLevel: Int = 3 // 1-5
)

enum class JournalPrivacy {
    PRIVATE, FRIENDS, SQUAD
}
