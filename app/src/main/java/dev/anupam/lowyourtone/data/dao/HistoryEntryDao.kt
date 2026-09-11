package dev.anupam.lowyourtone.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.anupam.lowyourtone.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryEntryDao {
    @Insert
    suspend fun insert(historyEntry: HistoryEntry)

    @Query("SELECT * FROM history_entries ORDER BY triggeredAt DESC")
    fun getAll(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_entries WHERE wakeWordId = :id")
    fun getByWakeWordId(id: String): Flow<List<HistoryEntry>>

    @Query("DELETE FROM history_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history_entries")
    fun getCount(): Flow<Int>

    @Query("SELECT triggeredAt FROM history_entries WHERE wakeWordId = :wakeWordId AND success = 1 ORDER BY triggeredAt DESC LIMIT 1")
    suspend fun getLastTriggerTime(wakeWordId: String): Long?
}
