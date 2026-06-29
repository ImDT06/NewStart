package com.example.newstart.data.local.dao

import androidx.room.*
import com.example.newstart.data.local.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals WHERE userId = :userId ORDER BY timestamp DESC")
    fun getJournalEntries(userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journals WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getJournalEntriesSync(userId: String): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournals(journals: List<JournalEntryEntity>)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournal(id: String)

    @Query("SELECT * FROM journals WHERE isSynced = 0")
    suspend fun getUnsyncedJournals(): List<JournalEntryEntity>

    @Query("SELECT id FROM journals WHERE userId = :userId AND isSynced = 1")
    suspend fun getSyncedJournalIds(userId: String): List<String>

    @Query("DELETE FROM journals WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}
