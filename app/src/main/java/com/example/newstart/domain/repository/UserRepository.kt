package com.example.newstart.domain.repository

import android.net.Uri
import com.example.newstart.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(userId: String): Flow<User>
    suspend fun updateAvatar(userId: String, uri: Uri?): Result<String?>
    suspend fun searchUsers(query: String): List<User>
}
