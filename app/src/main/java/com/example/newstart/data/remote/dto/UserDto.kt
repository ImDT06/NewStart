package com.example.newstart.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String? = null,
    val userId: String? = null,
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null
)
