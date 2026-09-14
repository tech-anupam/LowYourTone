package dev.anupam.lowyourtone.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ServiceState {
    LISTENING,
    STOPPED,
    ERROR
}

@Singleton
class ServiceStatusProvider @Inject constructor() {
    private val _state = MutableStateFlow(ServiceState.STOPPED)
    val state: StateFlow<ServiceState> = _state.asStateFlow()

    private val _lastTrigger = MutableStateFlow<String?>(null)
    val lastTrigger: StateFlow<String?> = _lastTrigger.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _wordCount = MutableStateFlow(0)
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    fun setListening(count: Int) {
        _state.value = ServiceState.LISTENING
        _wordCount.value = count
        _errorMessage.value = null
    }

    fun setStopped() {
        _state.value = ServiceState.STOPPED
        _wordCount.value = 0
    }

    fun setError(message: String) {
        _state.value = ServiceState.ERROR
        _errorMessage.value = message
    }

    fun setLastTrigger(description: String) {
        _lastTrigger.value = description
    }
}
