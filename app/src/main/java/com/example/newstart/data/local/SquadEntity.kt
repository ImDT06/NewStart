package com.example.newstart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.newstart.domain.model.Squad
import java.util.Date

@Entity(tableName = "squads")
data class SquadEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val habitCategory: String,
    val members: String, // Comma-separated list of Member User IDs
    val adminId: String,
    val coverImageUrl: String?,
    val createdAt: Long?,
    val cachedForUserId: String
)

fun SquadEntity.toDomain() = Squad(
    id = id,
    name = name,
    description = description,
    habitCategory = habitCategory,
    members = members.split(",").filter { it.isNotEmpty() },
    adminId = adminId,
    coverImageUrl = coverImageUrl,
    createdAt = createdAt?.let { Date(it) }
)

fun Squad.toEntity(cachedForUserId: String) = SquadEntity(
    id = id,
    name = name,
    description = description,
    habitCategory = habitCategory,
    members = members.joinToString(","),
    adminId = adminId,
    coverImageUrl = coverImageUrl,
    createdAt = createdAt?.time,
    cachedForUserId = cachedForUserId
)
