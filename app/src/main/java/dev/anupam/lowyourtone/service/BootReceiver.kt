package dev.anupam.lowyourtone.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.anupam.lowyourtone.prefs.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val prefs = PreferencesManager(context)
        val enabled = runBlocking { prefs.masterListeningEnabled.first() }
        if (enabled) {
            ContextCompat.startForegroundService(context, WakeWordService.createStartIntent(context))
        }
    }
}
