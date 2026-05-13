package com.example.newstart.domain.repository

import com.example.newstart.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(id: String): Flow<User>
}
