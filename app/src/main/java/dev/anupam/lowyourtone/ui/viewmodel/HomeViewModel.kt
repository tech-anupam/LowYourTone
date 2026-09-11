package dev.anupam.lowyourtone.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anupam.lowyourtone.data.dao.WakeActionDao
import dev.anupam.lowyourtone.data.dao.WakeWordDao
import dev.anupam.lowyourtone.data.model.WakeAction
import dev.anupam.lowyourtone.data.model.WakeWord
import dev.anupam.lowyourtone.prefs.PreferencesManager
import dev.anupam.lowyourtone.service.WakeWordService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val app: Application,
    private val wakeWordDao: WakeWordDao,
    private val wakeActionDao: WakeActionDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val masterListening: StateFlow<Boolean> = preferencesManager.masterListeningEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val wakeWordsWithActions: Flow<List<Pair<WakeWord, WakeAction?>>> = wakeWordDao.getAll()
        .combine(wakeActionDao.getAll()) { words, actions ->
            words.map { word -> word to actions.find { it.id == word.actionId } }
        }

    fun toggleMasterListening() {
        viewModelScope.launch {
            val next = !masterListening.value
            if (next) {
                if (app.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return@launch
                }
                preferencesManager.setMasterListeningEnabled(true)
                try {
                    app.startForegroundService(WakeWordService.createStartIntent(app))
                } catch (_: Exception) {
                    preferencesManager.setMasterListeningEnabled(false)
                }
            } else {
                preferencesManager.setMasterListeningEnabled(false)
                app.startService(WakeWordService.createStopIntent(app))
            }
        }
    }

    fun toggleWakeWord(id: String, enabled: Boolean) {
        viewModelScope.launch {
            wakeWordDao.getById(id)?.let { wakeWordDao.update(it.copy(enabled = enabled)) }
        }
    }

    fun deleteWakeWord(wakeWord: WakeWord) {
        viewModelScope.launch { wakeWordDao.delete(wakeWord) }
    }

    fun renameWakeWord(id: String, newPhrase: String) {
        viewModelScope.launch {
            wakeWordDao.getById(id)?.let { wakeWordDao.update(it.copy(phrase = newPhrase.trim().lowercase())) }
        }
    }
}
