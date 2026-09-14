package dev.anupam.lowyourtone.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class WakeWordService : Service(), WakeWordCallback {

    @Inject lateinit var wakeWordDao: WakeWordDao
    @Inject lateinit var wakeActionDao: WakeActionDao
    @Inject lateinit var actionDispatcher: ActionDispatcher
    @Inject lateinit var serviceStatus: ServiceStatusProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var engine: WakeWordEngine
    private var wakeLock: PowerManager.WakeLock? = null

    private val lastTriggerPerWord = ConcurrentHashMap<String, Long>()

    private val wakeLockRenewRunnable = object : Runnable {
        override fun run() {
            renewWakeLock()
            wakeLockHandler?.postDelayed(this, WAKE_LOCK_RENEW_MS)
        }
    }
    private var wakeLockHandler: android.os.Handler? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        engine = PocketSphinxEngine()
        engine.initialize(this, File(filesDir, "assets"))

        wakeLockHandler = android.os.Handler(mainLooper)

        wakeWordDao.getEnabled()
            .onEach { wakeWords ->
                val keywords = wakeWords.map { KeywordEntry(it.phrase, it.sensitivity) }
                engine.loadKeywords(keywords)
                if (keywords.isNotEmpty() && !engine.isListening()) {
                    engine.startListening(this@WakeWordService)
                }
                updateNotification(keywords.size)
                serviceStatus.setListening(keywords.size)
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                releaseWakeLock()
                engine.stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                serviceStatus.setStopped()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    serviceStatus.setError("Microphone permission not granted")
                    postResumeNotification("Microphone permission needed", "Tap to reopen LowYourTone and grant microphone access")
                    stopSelf()
                    return START_NOT_STICKY
                }
                try {
                    goForeground()
                } catch (e: Exception) {
                    postResumeNotification()
                    return START_NOT_STICKY
                }
                acquireWakeLock()
                if (!engine.isListening()) {
                    engine.startListening(this)
                }
                serviceStatus.setListening(0)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLockHandler?.removeCallbacks(wakeLockRenewRunnable)
        releaseWakeLock()
        engine.shutdown()
        serviceScope.cancel()
        serviceStatus.setStopped()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(this, WakeWordService::class.java).apply {
            action = ACTION_START
        }
        val pendingIntent = PendingIntent.getService(
            this,
            RESTART_REQUEST_CODE,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onWakeWordDetected(phrase: String, confidence: Float) {
        serviceScope.launch {
            val enabledWords = wakeWordDao.getEnabledList()
            val target = phrase.trim().lowercase()
            val wakeWord = enabledWords.find {
                val p = it.phrase.trim().lowercase()
                p == target
            } ?: return@launch

            val action = wakeActionDao.getById(wakeWord.actionId) ?: return@launch
            val phraseWordCount = wakeWord.phrase.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
            if (action.type == dev.anupam.lowyourtone.data.model.ActionType.CALL_EMERGENCY && phraseWordCount < 2) {
                postErrorNotification("Emergency trigger needs updating", "Use a unique two-word phrase to prevent accidental calls")
                return@launch
            }

            val now = System.currentTimeMillis()
            val wordKey = wakeWord.id
            val lastTime = lastTriggerPerWord[wordKey] ?: 0L
            val effectiveCooldown = maxOf(
                wakeWord.cooldownMs,
                if (action.type == dev.anupam.lowyourtone.data.model.ActionType.CALL_EMERGENCY) EMERGENCY_COOLDOWN_MS else 0L
            )
            if (effectiveCooldown > 0 && now - lastTime < effectiveCooldown) {
                return@launch
            }
            lastTriggerPerWord[wordKey] = now

            try {
                actionDispatcher.dispatch(wakeWord, action, confidence)
            } catch (e: Exception) {
                postErrorNotification("Action failed: ${action.label}", e.message ?: "Unknown error")
            }

            val triggerDesc = "${action.label} (${wakeWord.phrase})"
            serviceStatus.setLastTrigger(triggerDesc)
            updateNotificationStatus("Triggered: $triggerDesc")
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
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, dev.anupam.lowyourtone.MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                R.drawable.ic_app_logo,
                "STOP",
                PendingIntent.getService(
                    this,
                    STOP_ACTION_REQUEST,
                    createStopIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        manager.notify(NOTIFICATION_ID, notif)
    }

    override fun onListeningStarted() {
        serviceStatus.setListening(serviceStatus.wordCount.value)
    }

    override fun onListeningStopped() {}

    override fun onError(error: Exception) {
        serviceStatus.setError(error.message ?: "Recognition error")
        serviceScope.launch {
            kotlinx.coroutines.delay(2000)
            if (engine.isListening()) return@launch
            try {
                engine.startListening(this@WakeWordService)
                serviceStatus.setListening(serviceStatus.wordCount.value)
            } catch (e: Exception) {
                serviceStatus.setError("Failed to restart: ${e.message}")
            }
        }
    }

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
            wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
            wakeLockHandler?.postDelayed(wakeLockRenewRunnable, WAKE_LOCK_RENEW_MS)
        }
    }

    private fun renewWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
            it.acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLockHandler?.removeCallbacks(wakeLockRenewRunnable)
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
            val errorChannel = NotificationChannel(
                ERROR_CHANNEL_ID,
                "Errors & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(errorChannel)
        }
    }

    private fun createNotification(count: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Listening")
        .setContentText(buildNotificationText(count))
        .setSmallIcon(R.drawable.ic_app_logo)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, dev.anupam.lowyourtone.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            R.drawable.ic_app_logo,
            "STOP",
            PendingIntent.getService(
                this,
                STOP_ACTION_REQUEST,
                createStopIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun buildNotificationText(count: Int): String {
        val base = "Monitoring $count wake words"
        val lastTrigger = serviceStatus.lastTrigger.value
        return if (lastTrigger != null) "$base · Last: $lastTrigger" else base
    }

    private fun updateNotification(count: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(count))
    }

    private fun postResumeNotification(
        title: String = "LowYourTone paused",
        message: String = "Tap to resume listening"
    ) {
        val resumeIntent = PendingIntent.getActivity(
            this,
            RESUME_REQUEST_CODE,
            Intent(this, dev.anupam.lowyourtone.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, ERROR_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(resumeIntent)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(RESUME_NOTIFICATION_ID, notif)
    }

    private fun postErrorNotification(title: String, message: String) {
        val notif = NotificationCompat.Builder(this, ERROR_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, dev.anupam.lowyourtone.MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ERROR_NOTIFICATION_ID, notif)
    }

    companion object {
        private const val CHANNEL_ID = "listening_channel"
        private const val ERROR_CHANNEL_ID = "error_channel"
        private const val NOTIFICATION_ID = 1
        private const val RESUME_NOTIFICATION_ID = 2
        private const val ERROR_NOTIFICATION_ID = 99
        const val ACTION_START = "dev.anupam.lowyourtone.action.START"
        const val ACTION_STOP = "dev.anupam.lowyourtone.action.STOP"
        private const val RESTART_REQUEST_CODE = 42
        private const val RESUME_REQUEST_CODE = 43
        private const val STOP_ACTION_REQUEST = 44
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_RENEW_MS = 9 * 60 * 1000L
        private const val EMERGENCY_COOLDOWN_MS = 30_000L

        fun createStartIntent(context: Context) = Intent(context, WakeWordService::class.java).apply {
            action = ACTION_START
        }

        fun createStopIntent(context: Context) = Intent(context, WakeWordService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
