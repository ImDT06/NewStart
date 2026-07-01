package com.example.newstart.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HabitDto(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String? = null,
    val reminderTime: String?,
    val isCompleted: Boolean,
    val date: String,
    val userId: String,
    val squadId: String? = null
)
