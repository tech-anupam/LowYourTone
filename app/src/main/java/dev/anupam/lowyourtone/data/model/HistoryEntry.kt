package dev.anupam.lowyourtone.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_entries",
    indices = [Index(value = ["wakeWordId"]), Index(value = ["triggeredAt"])]
)
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wakeWordId: String,
    val matchedPhrase: String,
    val actionType: ActionType,
    val confidence: Float,
    val triggeredAt: Long,
    val success: Boolean,
    val failureReason: String?
)
