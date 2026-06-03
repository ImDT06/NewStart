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
    @ServerTimestamp
    val timestamp: Date? = null
)
