package com.example.newstart.domain.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@Immutable
@IgnoreExtraProperties
data class User(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null
)
