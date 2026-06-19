package com.example.newstart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.newstart.data.local.dao.HabitDao
import com.example.newstart.data.local.dao.TodoDao

@Database(entities = [HabitEntity::class, TodoEntity::class], version = 3, exportSchema = false)
abstract class NewStartDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao

    companion object {
        const val DATABASE_NAME = "newstart_db"
    }
}
