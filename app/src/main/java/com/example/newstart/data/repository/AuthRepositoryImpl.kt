package com.example.newstart.data.repository

import android.content.Context
import com.example.newstart.data.remote.ApiService
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.AuthRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override val currentUserId: String?
        get() = _currentUser.value?.id

    override val isEmailVerified: Boolean
        get() = true // Giả định là true đối với hệ thống custom API trừ khi có API check riêng

    init {
        // Khôi phục session từ SharedPreferences
        val userJson = sharedPrefs.getString("logged_in_user", null)
        if (userJson != null) {
            try {
                val user = gson.fromJson(userJson, User::class.java)
                _currentUser.value = user
            } catch (e: Exception) {
                sharedPrefs.edit().remove("logged_in_user").apply()
            }
        }
    }

    private fun saveUserToPrefs(user: User) {
        _currentUser.value = user
        sharedPrefs.edit().putString("logged_in_user", gson.toJson(user)).apply()
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val body = mapOf("email" to email, "password" to password)
            val user = apiService.login(body)
            saveUserToPrefs(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): Result<User> {
        return try {
            val body = mapOf("name" to name, "email" to email, "password" to password)
            val user = apiService.register(body)
            saveUserToPrefs(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            apiService.sendVerification()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            val body = mapOf("email" to email)
            apiService.resetPassword(body)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return try {
            val body = mapOf("idToken" to idToken)
            val user = apiService.loginWithGoogle(body)
            saveUserToPrefs(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        _currentUser.value = null
        sharedPrefs.edit().remove("logged_in_user").apply()
    }
}

