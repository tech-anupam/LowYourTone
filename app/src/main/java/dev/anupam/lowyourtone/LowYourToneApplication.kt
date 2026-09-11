package dev.anupam.lowyourtone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LowYourToneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val listeningChannel = NotificationChannel(
            "listening_channel",
            "Background Listening",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for background listening service"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        
        val detectionChannel = NotificationChannel(
            "detection_channel",
            "Wake Word Detected",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notification for when a wake word is detected"
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        
        val emergencyChannel = NotificationChannel(
            "emergency_channel",
            "Emergency Actions",
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = "Notification for emergency actions"
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        
        notificationManager.createNotificationChannel(listeningChannel)
        notificationManager.createNotificationChannel(detectionChannel)
        notificationManager.createNotificationChannel(emergencyChannel)
    }
}
