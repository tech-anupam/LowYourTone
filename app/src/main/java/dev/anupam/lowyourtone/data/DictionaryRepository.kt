package dev.anupam.lowyourtone.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    @Volatile
    private var cachedWords: Set<String>? = null

    fun loadList(): List<String> = getWordSet().toList()

    fun isValidWord(word: String): Boolean = getWordSet().contains(word.lowercase())

    @Synchronized
    private fun getWordSet(): Set<String> {
        cachedWords?.let { return it }
        val words = mutableSetOf<String>()
        try {
            context.assets.open("sync/models/lm/words.dic").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank() && !line.contains("(")) {
                        val word = line.substringBefore(" ").lowercase()
                        if (word.isNotEmpty()) words.add(word)
                    }
                }
            }
        } catch (_: Exception) {}
        cachedWords = words
        return words
    }
}
