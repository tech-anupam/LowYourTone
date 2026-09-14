package dev.anupam.lowyourtone.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anupam.lowyourtone.data.dao.HistoryEntryDao
import dev.anupam.lowyourtone.data.dao.WakeWordDao
import dev.anupam.lowyourtone.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyEntryDao: HistoryEntryDao,
    private val wakeWordDao: WakeWordDao,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val selectedWakeWordId = MutableStateFlow<String?>(null)

    val allWakeWords = wakeWordDao.getAll()

    val historyGrouped: Flow<Map<String, List<HistoryEntry>>> = selectedWakeWordId
        .flatMapLatest { wordId ->
            if (wordId == null) historyEntryDao.getAll() else historyEntryDao.getByWakeWordId(wordId)
        }
        .map { entries ->
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            entries.groupBy { format.format(Date(it.triggeredAt)) }
        }

    fun setFilter(wakeWordId: String?) {
        selectedWakeWordId.value = wakeWordId
    }

    suspend fun exportHistoryToCsv(): File? {
        val entries = historyEntryDao.getAll().first()
        if (entries.isEmpty()) return null
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "history_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { out ->
            out.write("ID,Timestamp,WakeWordId,Phrase,ActionType,Confidence,Success,FailureReason\n")
            entries.forEach { e ->
                out.write("${e.id},${e.triggeredAt},${e.wakeWordId},${e.matchedPhrase},${e.actionType},${e.confidence},${e.success},${e.failureReason ?: ""}\n")
            }
        }
        return file
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyEntryDao.deleteAll()
        }
    }
}
