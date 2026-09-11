package dev.anupam.lowyourtone.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.anupam.lowyourtone.R
import dev.anupam.lowyourtone.action.ActionDispatcher
import dev.anupam.lowyourtone.data.dao.WakeActionDao
import dev.anupam.lowyourtone.data.dao.WakeWordDao
import dev.anupam.lowyourtone.engine.KeywordEntry
import dev.anupam.lowyourtone.engine.PocketSphinxEngine
import dev.anupam.lowyourtone.engine.WakeWordCallback
import dev.anupam.lowyourtone.engine.WakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class WakeWordService : Service(), WakeWordCallback {

    @Inject lateinit var wakeWordDao: WakeWordDao
    @Inject lateinit var wakeActionDao: WakeActionDao
    @Inject lateinit var actionDispatcher: ActionDispatcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var engine: WakeWordEngine
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        engine = PocketSphinxEngine()
        engine.initialize(this, File(filesDir, "assets"))

        wakeWordDao.getEnabled()
            .onEach { wakeWords ->
                val keywords = wakeWords.map { KeywordEntry(it.phrase, it.sensitivity) }
                engine.loadKeywords(keywords)
                if (keywords.isNotEmpty() && !engine.isListening()) {
                    engine.startListening(this@WakeWordService)
                }
                updateNotification(keywords.size)
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                releaseWakeLock()
                engine.stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                try {
                    goForeground()
                } catch (e: Exception) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                acquireWakeLock()
                if (!engine.isListening()) {
                    engine.startListening(this)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        engine.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onWakeWordDetected(phrase: String, confidence: Float) {
        serviceScope.launch {
            val enabledWords = wakeWordDao.getEnabledList()
            val target = phrase.trim().lowercase()
            val wakeWord = enabledWords.find {
                val p = it.phrase.trim().lowercase()
                p == target || target.contains(p) || p.contains(target) || (p.contains(" ") && p.split(" ").all { word -> target.contains(word) })
            } ?: wakeWordDao.getByPhrase(target) ?: return@launch

            val action = wakeActionDao.getById(wakeWord.actionId) ?: return@launch
            actionDispatcher.dispatch(wakeWord, action, confidence)
            updateNotificationStatus("Triggered: ${action.label} (${wakeWord.phrase})")
        }
    }

    private fun updateNotificationStatus(status: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LowYourTone")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, dev.anupam.lowyourtone.MainActivity::class.java),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        manager.notify(NOTIFICATION_ID, notif)
    }

    override fun onListeningStarted() {}
    override fun onListeningStopped() {}
    override fun onError(error: Exception) {}

    private fun goForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LowYourTone::Listening")
            wakeLock?.acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Listening Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(count: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Listening")
        .setContentText("Monitoring $count wake words")
        .setSmallIcon(R.drawable.ic_app_logo)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(
            android.app.PendingIntent.getActivity(
                this,
                0,
                Intent(this, dev.anupam.lowyourtone.MainActivity::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun updateNotification(count: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(count))
    }

    companion object {
        private const val CHANNEL_ID = "listening_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "dev.anupam.lowyourtone.action.START"
        const val ACTION_STOP = "dev.anupam.lowyourtone.action.STOP"

        fun createStartIntent(context: Context) = Intent(context, WakeWordService::class.java).apply {
            action = ACTION_START
        }

        fun createStopIntent(context: Context) = Intent(context, WakeWordService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
