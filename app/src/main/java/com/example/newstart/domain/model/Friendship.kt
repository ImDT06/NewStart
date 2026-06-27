package com.example.newstart.domain.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Immutable
data class Friendship(
    val id: String = "",
    val userIds: List<String> = emptyList(), // Danh sách 2 ID người dùng
    @ServerTimestamp
    val createdAt: Date? = null,
    val status: String = "ACCEPTED"
)

@Immutable
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: String = "PENDING",
    @ServerTimestamp
    val timestamp: Date? = null
)

enum class FriendshipStatus {
    PENDING, ACCEPTED, REJECTED
}
