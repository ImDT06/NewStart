package com.example.newstart.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TodoDto(
    val id: String? = null,
    val task: String,
    val isCompleted: Boolean,
    val priority: String,
    val dueDate: Long? = null,
    val createdAt: Long? = null,
    val userId: String? = null
)
