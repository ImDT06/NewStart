package com.example.newstart.domain.model

import java.util.Date

data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val emoji: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Date = Date()
)
