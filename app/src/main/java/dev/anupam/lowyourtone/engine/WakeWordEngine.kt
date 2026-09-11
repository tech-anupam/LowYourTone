package dev.anupam.lowyourtone.engine

import android.content.Context
import java.io.File

interface WakeWordEngine {
    fun initialize(context: Context, assetsDir: File)
    fun loadKeywords(keywords: List<KeywordEntry>)
    fun startListening(callback: WakeWordCallback)
    fun stopListening()
    fun shutdown()
    fun isListening(): Boolean
}
