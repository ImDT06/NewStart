package com.example.newstart.domain.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null
)
