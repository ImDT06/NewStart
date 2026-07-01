package com.example.newstart.domain.model

import java.util.Date

data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val timestamp: Date? = null,
    val sharedJournalId: String? = null,
    val sharedJournalText: String? = null,
    val sharedJournalImageUrl: String? = null,
    val sharedJournalEmoji: String? = null,
    val sharedJournalAuthorName: String? = null
)
