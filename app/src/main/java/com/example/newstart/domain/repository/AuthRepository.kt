package com.example.newstart.domain.repository

import com.example.newstart.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isEmailVerified: Boolean
    
    suspend fun loginWithEmail(email: String, password: String): Result<User>
    suspend fun registerWithEmail(name: String, email: String, password: String): Result<User>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun logout()
    suspend fun checkIsAdmin(): Boolean
}
