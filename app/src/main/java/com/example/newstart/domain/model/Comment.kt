package com.example.newstart.domain.model

import java.util.Date

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val text: String = "",
    val timestamp: Date? = null
)
