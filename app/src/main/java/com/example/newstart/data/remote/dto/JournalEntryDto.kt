package com.example.newstart.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class MovieDetailsDto(
    val title: String = "",
    val director: String = "",
    val actors: String = "",
    val rating: Float = 0f
)

@Serializable
data class BookDetailsDto(
    val title: String = "",
    val author: String = "",
    val pagesRead: Int = 0,
    val rating: Float = 0f
)

@Serializable
data class SubjectDetailsDto(
    val name: String = "",
    val topic: String = "",
    val score: Float? = null,
    val understandingLevel: Int = 3
)

@Serializable
data class JournalEntryDto(
    val id: String? = null,
    val userId: String? = null,
    val emoji: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val imageSource: String? = null,
    val type: String = "NORMAL",
    val privacy: String = "FRIENDS",
    val movieDetails: MovieDetailsDto? = null,
    val bookDetails: BookDetailsDto? = null,
    val subjectDetails: SubjectDetailsDto? = null,
    val reactions: Map<String, String> = emptyMap(),
    val timestamp: JsonElement? = null
)

fun JsonElement?.toEpochMilliseconds(): Long? {
    if (this == null || this is JsonNull) return null
    return try {
        when (this) {
            is JsonPrimitive -> this.longOrNull
            is JsonObject -> {
                val seconds = this["seconds"]?.jsonPrimitive?.longOrNull
                seconds?.let { it * 1000 }
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
