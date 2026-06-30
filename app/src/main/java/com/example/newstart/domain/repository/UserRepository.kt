package com.example.newstart.domain.repository

import android.net.Uri
import com.example.newstart.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(userId: String): Flow<User>
    suspend fun updateAvatar(userId: String, uri: Uri?): Result<String?>
    suspend fun updateProfile(userId: String, name: String): Result<Unit>
    suspend fun searchUsers(query: String): List<User>
    suspend fun updateFcmToken(token: String): Result<Unit>
    suspend fun updateProfileFields(updates: Map<String, String>): Result<Unit>
    suspend fun updateEmail(newEmail: String): Result<Unit>
    fun getAllUsers(): Flow<List<User>>
    suspend fun blockUser(userId: String, block: Boolean): Result<Unit>
    fun getBlockedUsers(): Flow<Set<String>>
}
