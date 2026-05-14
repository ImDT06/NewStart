package com.example.newstart.di

import com.example.newstart.data.repository.AuthRepositoryImpl
import com.example.newstart.data.repository.JournalRepositoryImpl
import com.example.newstart.data.repository.UserRepositoryImpl
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.JournalRepository
import com.example.newstart.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindJournalRepository(
        journalRepositoryImpl: JournalRepositoryImpl
    ): JournalRepository
}
