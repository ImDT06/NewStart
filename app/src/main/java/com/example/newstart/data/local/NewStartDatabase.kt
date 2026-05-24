package com.example.newstart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.newstart.data.local.dao.HabitDao

@Database(entities = [HabitEntity::class], version = 1, exportSchema = false)
abstract class NewStartDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        const val DATABASE_NAME = "newstart_db"
    }
}
