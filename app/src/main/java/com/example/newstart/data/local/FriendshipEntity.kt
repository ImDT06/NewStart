package com.example.newstart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newstart.domain.model.Friendship
import java.util.Date

@Entity(tableName = "friendships")
data class FriendshipEntity(
    @PrimaryKey
    val id: String,
    val userIds: String, // Comma-separated list of User IDs
    val createdAt: Long?,
    val status: String,
    val cachedForUserId: String
)

fun FriendshipEntity.toDomain() = Friendship(
    id = id,
    userIds = userIds.split(",").filter { it.isNotEmpty() },
    createdAt = createdAt?.let { Date(it) },
    status = status
)

fun Friendship.toEntity(cachedForUserId: String) = FriendshipEntity(
    id = id,
    userIds = userIds.joinToString(","),
    createdAt = createdAt?.time,
    status = status,
    cachedForUserId = cachedForUserId
)
