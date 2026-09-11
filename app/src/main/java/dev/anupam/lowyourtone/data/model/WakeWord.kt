package dev.anupam.lowyourtone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wake_words")
data class WakeWord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val phrase: String,
    val enabled: Boolean = true,
    val sensitivity: Float = 1e-20f,
    val actionId: String,
    val requireConfirmation: Boolean = false,
    val cooldownMs: Long = 5000L,
    val createdAt: Long = System.currentTimeMillis()
)
