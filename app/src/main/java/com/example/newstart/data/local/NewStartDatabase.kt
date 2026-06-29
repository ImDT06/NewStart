package com.example.newstart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.dao.TodoDao
import com.example.newstart.data.local.dao.JournalDao
import com.example.newstart.data.local.dao.SocialDao
import com.example.newstart.data.local.dao.UserDao

@Database(entities = [HabitEntity::class, TodoEntity::class, JournalEntryEntity::class, FriendshipEntity::class, SquadEntity::class, UserEntity::class], version = 6, exportSchema = false)
abstract class NewStartDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun journalDao(): JournalDao
    abstract fun socialDao(): SocialDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "newstart_db"
    }
}


