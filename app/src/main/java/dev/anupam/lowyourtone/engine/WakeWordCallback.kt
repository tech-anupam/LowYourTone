package dev.anupam.lowyourtone.engine

interface WakeWordCallback {
    fun onWakeWordDetected(phrase: String, confidence: Float)
    fun onListeningStarted()
    fun onListeningStopped()
    fun onError(error: Exception)
}
