package com.example.newstart.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SquadMessageDto(
    val id: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val text: String,
    val timestamp: JsonElement? = null
)
