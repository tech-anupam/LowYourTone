package dev.anupam.lowyourtone.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wake_actions")
data class WakeAction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val label: String,
    val iconRes: String,
    val paramsJson: String
)
