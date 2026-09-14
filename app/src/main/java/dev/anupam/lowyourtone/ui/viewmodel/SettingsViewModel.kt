package dev.anupam.lowyourtone.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anupam.lowyourtone.prefs.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val defaultSensitivity: StateFlow<Int> = preferencesManager.defaultSensitivity
        .stateIn(viewModelScope, SharingStarted.Lazily, 5)
        
    val silentMode: StateFlow<Boolean> = preferencesManager.silentMode
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val masterListeningEnabled: StateFlow<Boolean> = preferencesManager.masterListeningEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun isBatteryOptimized(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun setDefaultSensitivity(value: Int) {
        viewModelScope.launch {
            preferencesManager.setDefaultSensitivity(value)
        }
    }

    fun toggleSilentMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSilentMode(enabled)
        }
    }
}
