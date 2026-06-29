package com.example.newstart.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SquadDto(
    val id: String? = null,
    val name: String,
    val description: String,
    val habitCategory: String = "",
    val members: List<String> = emptyList(),
    val adminId: String? = null,
    val coverImageUrl: String? = null,
    val createdAt: JsonElement? = null
)
