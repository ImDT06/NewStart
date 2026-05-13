package com.example.newstart.data.repository

import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {
    override fun getUserById(id: String): Flow<User> = flow {
        emit(User(id = id, name = "Hilt User", email = "hilt@example.com"))
    }
}
