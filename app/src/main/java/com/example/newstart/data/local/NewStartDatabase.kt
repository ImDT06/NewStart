package com.example.newstart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.dao.TodoDao
import com.example.newstart.data.local.dao.JournalDao

@Database(entities = [HabitEntity::class, TodoEntity::class, JournalEntryEntity::class], version = 4, exportSchema = false)
abstract class NewStartDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun journalDao(): JournalDao

    companion object {
        const val DATABASE_NAME = "newstart_db"
    }
}
