package dev.anupam.lowyourtone.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.anupam.lowyourtone.MainActivity
import dev.anupam.lowyourtone.R
import dev.anupam.lowyourtone.prefs.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val prefs = PreferencesManager(context)
        val enabled = runBlocking { prefs.masterListeningEnabled.first() }
        if (enabled) {
            try {
                ContextCompat.startForegroundService(context, WakeWordService.createStartIntent(context))
            } catch (e: Exception) {
                postResumeNotification(context)
            }
        }
    }

    private fun postResumeNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "error_channel",
                "Errors & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val resumeIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, "error_channel")
            .setContentTitle("LowYourTone needs attention")
            .setContentText("Tap to resume listening after reboot")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(resumeIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notif)
    }
}
