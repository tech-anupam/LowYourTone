package dev.anupam.lowyourtone.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anupam.lowyourtone.data.DictionaryRepository
import dev.anupam.lowyourtone.data.dao.WakeActionDao
import dev.anupam.lowyourtone.data.dao.WakeWordDao
import dev.anupam.lowyourtone.data.model.ActionType
import dev.anupam.lowyourtone.data.model.WakeAction
import dev.anupam.lowyourtone.data.model.WakeWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditWakeWordViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val wakeWordDao: WakeWordDao,
    private val wakeActionDao: WakeActionDao,
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val wakeWordId: String? = savedStateHandle.get<String>("wakeWordId")
    private val gson = Gson()

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        if (wakeWordId != null) {
            viewModelScope.launch {
                val word = wakeWordDao.getById(wakeWordId)
                if (word != null) {
                    val action = wakeActionDao.getById(word.actionId)
                    val params = action?.let { parseParams(it.paramsJson) } ?: emptyMap()
                    _uiState.update {
                        it.copy(
                            isEditing = true,
                            phrase = word.phrase,
                            requireConfirmation = word.requireConfirmation,
                            cooldownSeconds = (word.cooldownMs / 1000).toInt(),
                            selectedActionType = action?.type,
                            actionParams = params
                        )
                    }
                    validatePhrase(word.phrase)
                }
            }
        }
    }

    fun updatePhrase(phrase: String) {
        _uiState.update { it.copy(phrase = phrase) }
        validatePhrase(phrase)
    }

    fun updateRequireConfirmation(require: Boolean) {
        _uiState.update { it.copy(requireConfirmation = require) }
    }

    fun updateCooldown(cooldown: Int) {
        _uiState.update { it.copy(cooldownSeconds = cooldown) }
    }

    fun updateActionParams(type: ActionType, params: Map<String, String>) {
        _uiState.update {
            it.copy(
                selectedActionType = type,
                actionParams = params,
                requireConfirmation = if (type == ActionType.CALL_EMERGENCY) false else it.requireConfirmation,
                cooldownSeconds = if (type == ActionType.CALL_EMERGENCY) 0 else it.cooldownSeconds
            )
        }
    }

    fun updateSensitivity(value: Int) {
        _uiState.update { it.copy(sensitivity = value) }
    }

    private fun validatePhrase(phrase: String) {
        viewModelScope.launch {
            val words = phrase.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (phrase.length < 3) {
                _uiState.update { it.copy(phraseWarning = "Phrase must be at least 3 characters") }
                return@launch
            }
            val allValid = words.all { dictionaryRepository.isValidWord(it) }
            _uiState.update {
                it.copy(phraseWarning = if (allValid) null else "Warning: Some words not in dictionary - detection may be unreliable")
            }
        }
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.phrase.isBlank()) return

        viewModelScope.launch {
            val existingWord = wakeWordId?.let { wakeWordDao.getById(it) }
            val actionId = if (state.isEditing && existingWord != null) existingWord.actionId else UUID.randomUUID().toString()
            val wordId = wakeWordId ?: UUID.randomUUID().toString()
            val wordCount = state.phrase.trim().split("\\s+".toRegex()).size
            val threshold = when {
                wordCount <= 1 -> 1e-25f
                wordCount == 2 -> 1e-35f
                else -> 1e-45f
            }

            state.selectedActionType?.let { type ->
                val action = WakeAction(
                    id = actionId,
                    type = type,
                    label = type.label,
                    iconRes = "",
                    paramsJson = gson.toJson(state.actionParams)
                )
                wakeActionDao.insert(action)
            }

            val word = WakeWord(
                id = wordId,
                phrase = state.phrase.trim().lowercase(),
                sensitivity = threshold,
                actionId = state.selectedActionType?.let { actionId } ?: "",
                requireConfirmation = if (state.selectedActionType == ActionType.CALL_EMERGENCY) false else state.requireConfirmation,
                cooldownMs = if (state.selectedActionType == ActionType.CALL_EMERGENCY) 0L else state.cooldownSeconds * 1000L,
                enabled = true
            )

            if (wakeWordId == null) {
                wakeWordDao.insert(word)
            } else {
                wakeWordDao.update(word)
            }

            onDone()
        }
    }

    private fun parseParams(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
}

data class AddEditUiState(
    val isEditing: Boolean = false,
    val phrase: String = "",
    val phraseWarning: String? = null,
    val requireConfirmation: Boolean = false,
    val cooldownSeconds: Int = 5,
    val selectedActionType: ActionType? = null,
    val actionParams: Map<String, String> = emptyMap(),
    val sensitivity: Int = 5
)
