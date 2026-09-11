package dev.anupam.lowyourtone.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.anupam.lowyourtone.data.model.WakeWord
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeWordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wakeWord: WakeWord)

    @Update
    suspend fun update(wakeWord: WakeWord)

    @Delete
    suspend fun delete(wakeWord: WakeWord)

    @Query("SELECT * FROM wake_words ORDER BY createdAt DESC")
    fun getAll(): Flow<List<WakeWord>>

    @Query("SELECT * FROM wake_words WHERE enabled = 1")
    fun getEnabled(): Flow<List<WakeWord>>

    @Query("SELECT * FROM wake_words WHERE enabled = 1")
    suspend fun getEnabledList(): List<WakeWord>

    @Query("SELECT * FROM wake_words WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WakeWord?

    @Query("SELECT * FROM wake_words WHERE phrase = :phrase LIMIT 1")
    suspend fun getByPhrase(phrase: String): WakeWord?
}
