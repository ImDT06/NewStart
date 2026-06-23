package com.example.newstart.domain.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Immutable
@IgnoreExtraProperties
data class Todo(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val task: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)

enum class Priority {
    LOW, MEDIUM, HIGH
}
