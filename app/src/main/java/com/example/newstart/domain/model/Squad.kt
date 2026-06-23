package com.example.newstart.domain.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Immutable
@IgnoreExtraProperties
data class Squad(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val habitCategory: String = "", // Ví dụ: "Thức dậy sớm", "Chạy bộ"
    val members: List<String> = emptyList(), // Danh sách UserIDs
    val adminId: String = "",
    val coverImageUrl: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)
