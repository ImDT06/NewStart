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
    val privacy: JournalPrivacy = JournalPrivacy.PRIVATE, // Quyền riêng tư
    @ServerTimestamp
    val timestamp: Date? = null
)

enum class JournalPrivacy {
    PRIVATE, FRIENDS, SQUAD
}
