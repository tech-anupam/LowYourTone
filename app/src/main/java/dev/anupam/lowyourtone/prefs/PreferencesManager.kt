package dev.anupam.lowyourtone.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dev.anupam.lowyourtone.prefs")

class PreferencesManager(private val context: Context) {

    private val masterListeningEnabledKey = booleanPreferencesKey("master_listening_enabled")
    private val defaultSensitivityKey = intPreferencesKey("default_sensitivity")
    private val silentModeKey = booleanPreferencesKey("silent_mode")
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")

    val masterListeningEnabled: Flow<Boolean> = context.dataStore.data.map { it[masterListeningEnabledKey] ?: false }
    val defaultSensitivity: Flow<Int> = context.dataStore.data.map { it[defaultSensitivityKey] ?: 3 }
    val silentMode: Flow<Boolean> = context.dataStore.data.map { it[silentModeKey] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[onboardingCompleteKey] ?: false }

    suspend fun setMasterListeningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[masterListeningEnabledKey] = enabled }
    }

    suspend fun setDefaultSensitivity(sensitivity: Int) {
        context.dataStore.edit { it[defaultSensitivityKey] = sensitivity }
    }

    suspend fun setSilentMode(enabled: Boolean) {
        context.dataStore.edit { it[silentModeKey] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardingCompleteKey] = complete }
    }
}
