package dev.anupam.lowyourtone.action

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.anupam.lowyourtone.data.dao.WakeActionDao
import dev.anupam.lowyourtone.data.dao.WakeWordDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmActionReceiver : BroadcastReceiver() {

    @Inject lateinit var wakeWordDao: WakeWordDao
    @Inject lateinit var wakeActionDao: WakeActionDao
    @Inject lateinit var actionDispatcher: ActionDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        val wakeWordId = intent.getStringExtra("wake_word_id") ?: return
        val actionId = intent.getStringExtra("action_id") ?: return
        val confidence = intent.getFloatExtra("confidence", 0f)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val wakeWord = wakeWordDao.getById(wakeWordId) ?: return@launch
                val action = wakeActionDao.getById(actionId) ?: return@launch
                actionDispatcher.dispatch(wakeWord.copy(requireConfirmation = false), action, confidence)
            } finally {
                pending.finish()
            }
        }
    }
}
