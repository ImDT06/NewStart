package com.example.newstart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newstart.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String?
)

fun UserEntity.toDomain() = User(
    id = id,
    userId = userId,
    name = name,
    email = email,
    avatarUrl = avatarUrl
)

fun User.toEntity() = UserEntity(
    id = id,
    userId = userId,
    name = name,
    email = email,
    avatarUrl = avatarUrl
)
