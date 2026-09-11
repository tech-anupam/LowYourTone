package dev.anupam.lowyourtone.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.anupam.lowyourtone.data.dao.HistoryEntryDao
import dev.anupam.lowyourtone.data.dao.WakeActionDao
import dev.anupam.lowyourtone.data.dao.WakeWordDao
import dev.anupam.lowyourtone.data.model.HistoryEntry
import dev.anupam.lowyourtone.data.model.WakeAction
import dev.anupam.lowyourtone.data.model.WakeWord

@Database(
    entities = [WakeWord::class, WakeAction::class, HistoryEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wakeWordDao(): WakeWordDao
    abstract fun wakeActionDao(): WakeActionDao
    abstract fun historyEntryDao(): HistoryEntryDao
}
