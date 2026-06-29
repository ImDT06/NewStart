package com.example.newstart.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipDto(
    val id: String,
    val userIds: List<String>,
    val status: String = "ACCEPTED"
)
