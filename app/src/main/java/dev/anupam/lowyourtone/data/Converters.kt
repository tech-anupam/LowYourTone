package dev.anupam.lowyourtone.data

import androidx.room.TypeConverter
import dev.anupam.lowyourtone.data.model.ActionType

class Converters {
    @TypeConverter
    fun fromActionType(value: ActionType): String = value.name

    @TypeConverter
    fun toActionType(value: String): ActionType = ActionType.valueOf(value)
}
