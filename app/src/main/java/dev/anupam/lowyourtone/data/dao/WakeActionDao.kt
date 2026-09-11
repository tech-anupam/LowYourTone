package dev.anupam.lowyourtone.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.anupam.lowyourtone.data.model.WakeAction
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wakeAction: WakeAction)

    @Update
    suspend fun update(wakeAction: WakeAction)

    @Delete
    suspend fun delete(wakeAction: WakeAction)

    @Query("SELECT * FROM wake_actions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WakeAction?

    @Query("SELECT * FROM wake_actions")
    fun getAll(): Flow<List<WakeAction>>
}
