package dev.anupam.lowyourtone.di

import android.content.Context
import android.hardware.SensorPrivacyManager
import android.os.Build
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.anupam.lowyourtone.data.AppDatabase
import dev.anupam.lowyourtone.data.dao.HistoryEntryDao
import dev.anupam.lowyourtone.data.dao.WakeActionDao
import dev.anupam.lowyourtone.data.dao.WakeWordDao
import dev.anupam.lowyourtone.prefs.PreferencesManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager = PreferencesManager(context)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "lowyourtone.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideWakeWordDao(database: AppDatabase): WakeWordDao = database.wakeWordDao()

    @Provides
    fun provideWakeActionDao(database: AppDatabase): WakeActionDao = database.wakeActionDao()

    @Provides
    fun provideHistoryEntryDao(database: AppDatabase): HistoryEntryDao = database.historyEntryDao()

    @Provides
    @Singleton
    fun provideSensorPrivacyManager(@ApplicationContext context: Context): SensorPrivacyManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SensorPrivacyManager::class.java)
        } else {
            null
        }
}
