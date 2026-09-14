package dev.anupam.lowyourtone.action

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.anupam.lowyourtone.data.dao.HistoryEntryDao
import dev.anupam.lowyourtone.data.dao.WakeActionDao
import dev.anupam.lowyourtone.data.model.ActionType
import dev.anupam.lowyourtone.data.model.HistoryEntry
import dev.anupam.lowyourtone.data.model.WakeAction
import dev.anupam.lowyourtone.data.model.WakeWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionDispatcher @Inject constructor(
    private val context: Application,
    private val historyEntryDao: HistoryEntryDao,
    private val wakeActionDao: WakeActionDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var activeRecorder: MediaRecorder? = null
    private var activePlayer: MediaPlayer? = null
    private var flashlightOn = false

    init {
        createNotificationChannels()
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    flashlightOn = enabled
                }
            }, null)
        } catch (_: Exception) {}
    }

    fun dispatch(wakeWord: WakeWord, action: WakeAction, confidence: Float) {
        scope.launch {
            val minCooldown = when (action.type) {
                ActionType.CALL_EMERGENCY, ActionType.STOP_MEDIA -> 0L
                ActionType.TOGGLE_FLASHLIGHT -> 800L
                else -> wakeWord.cooldownMs
            }
            if (minCooldown > 0L) {
                val lastTrigger = historyEntryDao.getLastTriggerTime(wakeWord.id)
                if (lastTrigger != null && System.currentTimeMillis() - lastTrigger < minCooldown) {
                    return@launch
                }
            }

            if (wakeWord.requireConfirmation && action.type != ActionType.CALL_EMERGENCY) {
                showConfirmationNotification(wakeWord, action, confidence)
            } else {
                executeAction(wakeWord, action, confidence)
            }
        }
    }

    private suspend fun executeAction(wakeWord: WakeWord, action: WakeAction, confidence: Float) {
        vibrateOnDetection()
        showDetectionNotification(wakeWord, action)
        val params = parseParams(action.paramsJson)
        try {
            when (action.type) {
                ActionType.CALL_CONTACT -> handleCallContact(params)
                ActionType.CALL_EMERGENCY -> handleCallEmergency(params)
                ActionType.SEND_SMS -> handleSendSms(params)
                ActionType.START_AUDIO_RECORD -> handleStartAudioRecord(params)
                ActionType.START_VIDEO_RECORD -> handleStartVideoRecord()
                ActionType.SEND_LOCATION_SMS -> handleSendLocationSms(params)
                ActionType.OPEN_APP -> handleOpenApp(params)
                ActionType.TOGGLE_FLASHLIGHT -> handleToggleFlashlight()
                ActionType.PLAY_ALARM_SOUND -> handlePlayAlarmSound(params)
                ActionType.SEND_WHATSAPP -> handleSendWhatsapp(params)
                ActionType.CUSTOM_INTENT -> handleCustomIntent(params)
                ActionType.LOCK_SCREEN -> handleLockScreen()
                ActionType.TOGGLE_SILENT -> handleToggleSilent()
                ActionType.TOGGLE_DND -> handleToggleDnd()
                ActionType.MAX_VOLUME -> handleMaxVolume()
                ActionType.STOP_MEDIA -> handleStopMedia()
                ActionType.DISCO_FLASH -> handleDiscoFlash(params)
            }
            recordHistory(wakeWord, action, confidence, true, null)
        } catch (e: Exception) {
            recordHistory(wakeWord, action, confidence, false, e.message)
            showActionErrorNotification(action.label, e.message ?: "Action failed unexpectedly")
            if (action.type == ActionType.CALL_EMERGENCY) {
                showEmergencyFailureNotification(e.message ?: "Emergency action failed")
            }
        }
    }

    private fun requirePermission(permission: String) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("$permission not granted")
        }
    }

    private fun handleCallContact(params: Map<String, Any>) {
        requirePermission(Manifest.permission.CALL_PHONE)
        val number = params["phoneNumber"] as? String ?: throw IllegalArgumentException("No phone number")
        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun handleCallEmergency(params: Map<String, Any>) {
        requirePermission(Manifest.permission.CALL_PHONE)
        val number = params["number"] as? String ?: throw IllegalArgumentException("No emergency number")
        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        val sendLocation = params["sendLocationSms"] as? Boolean ?: false
        if (sendLocation) {
            val contact = params["emergencyContact"] as? String ?: return
            scope.launch { sendLocationSms(contact) }
        }
    }

    private fun handleSendSms(params: Map<String, Any>) {
        val number = params["phoneNumber"] as? String ?: throw IllegalArgumentException("No phone number")
        val template = params["messageTemplate"] as? String ?: ""
        val message = expandTemplate(template)
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                getSmsManager().sendTextMessage(number, null, message, null, null)
            } else {
                throw SecurityException("SMS permission not granted")
            }
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun handleStartAudioRecord(params: Map<String, Any>) {
        requirePermission(Manifest.permission.RECORD_AUDIO)
        stopActiveRecorder()
        val durationSec = (params["durationSeconds"] as? Double)?.toLong() ?: 30L
        val file = File(context.getExternalFilesDir("recordings"), "audio_${System.currentTimeMillis()}.3gp")
        file.parentFile?.mkdirs()
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(file.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setMaxDuration((durationSec * 1000).toInt())
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopActiveRecorder()
                }
            }
            prepare()
            start()
        }
        activeRecorder = recorder
    }

    private fun handleStartVideoRecord() {
        context.startActivity(Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun handleSendLocationSms(params: Map<String, Any>) {
        requirePermission(Manifest.permission.SEND_SMS)
        val number = params["phoneNumber"] as? String ?: throw IllegalArgumentException("No phone number")
        scope.launch { sendLocationSms(number) }
    }

    private fun handleOpenApp(params: Map<String, Any>) {
        val packageName = params["packageName"] as? String ?: throw IllegalArgumentException("No package name")
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalArgumentException("App not found: $packageName")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun handleToggleFlashlight() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            try {
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } catch (_: Exception) { false }
        } ?: cameraManager.cameraIdList.firstOrNull() ?: throw IllegalStateException("No camera with flash")
        flashlightOn = !flashlightOn
        try {
            cameraManager.setTorchMode(cameraId, flashlightOn)
        } catch (e: Exception) {
            flashlightOn = !flashlightOn
            throw e
        }
    }

    private fun handlePlayAlarmSound(params: Map<String, Any>) {
        stopActivePlayer()
        val durationSec = (params["durationSeconds"] as? Double)?.toLong() ?: 10L
        val volume = (params["volumeOverride"] as? Double)?.toFloat() ?: 1.0f
        val player = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            ?: throw IllegalStateException("Could not create alarm player")
        player.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setVolume(volume, volume)
            isLooping = false
            start()
        }
        activePlayer = player
        scope.launch {
            delay(durationSec * 1000)
            stopActivePlayer()
        }
    }

    private fun handleSendWhatsapp(params: Map<String, Any>) {
        val number = params["phoneNumber"] as? String ?: throw IllegalArgumentException("No phone number")
        val text = params["messageTemplate"] as? String ?: ""
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=${Uri.encode(expandTemplate(text))}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun handleCustomIntent(params: Map<String, Any>) {
        val action = params["action"] as? String ?: throw IllegalArgumentException("No action")
        val pkg = params["package"] as? String
        @Suppress("UNCHECKED_CAST")
        val extras = params["extras"] as? Map<String, Any>
        context.startActivity(Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            pkg?.let { setPackage(it) }
            extras?.forEach { (key, value) ->
                when (value) {
                    is String -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    is Double -> putExtra(key, value)
                }
            }
        })
    }

    private fun handleLockScreen() {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, context.packageName + ".DeviceAdminReceiver")
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
            } else {
                val accessibilityIntent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                showActionErrorNotification("Lock Screen", "Device admin not active. Enable it in Settings to use screen lock.")
                context.startActivity(accessibilityIntent)
            }
        } catch (e: Exception) {
            showActionErrorNotification("Lock Screen", "Unable to lock screen: ${e.message}")
        }
    }

    private fun handleToggleSilent() {
        val current = audioManager.ringerMode
        audioManager.ringerMode = if (current == AudioManager.RINGER_MODE_SILENT) {
            AudioManager.RINGER_MODE_NORMAL
        } else {
            AudioManager.RINGER_MODE_SILENT
        }
    }

    private fun handleToggleDnd() {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            val current = notificationManager.currentInterruptionFilter
            notificationManager.setInterruptionFilter(
                if (current == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
            )
        } else {
            context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    private fun handleMaxVolume() {
        listOf(
            AudioManager.STREAM_RING,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_NOTIFICATION
        ).forEach { stream ->
            audioManager.setStreamVolume(stream, audioManager.getStreamMaxVolume(stream), 0)
        }
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }

    private fun handleStopMedia() {
        stopActiveRecorder()
        stopActivePlayer()
        flashlightOn = false
        try {
            val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cam.cameraIdList.firstOrNull()?.let { cam.setTorchMode(it, false) }
        } catch (_: Exception) {}
    }

    private fun handleDiscoFlash(params: Map<String, Any>) {
        val durationSec = (params["durationSeconds"] as? Double)?.toLong() ?: 10L
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            try {
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } catch (_: Exception) { false }
        } ?: cameraManager.cameraIdList.firstOrNull() ?: throw IllegalStateException("No camera with flash")
        scope.launch {
            val endTime = System.currentTimeMillis() + (durationSec * 1000)
            var on = false
            while (System.currentTimeMillis() < endTime) {
                on = !on
                try {
                    cameraManager.setTorchMode(cameraId, on)
                } catch (_: Exception) {}
                if (on) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(
                                android.os.VibrationEffect.createOneShot(70, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                        }
                    } catch (_: Exception) {}
                }
                delay(120)
            }
            try {
                cameraManager.setTorchMode(cameraId, false)
            } catch (_: Exception) {}
            flashlightOn = false
        }
    }

    @Synchronized
    private fun stopActiveRecorder() {
        activeRecorder?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        activeRecorder = null
    }

    @Synchronized
    private fun stopActivePlayer() {
        activePlayer?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        activePlayer = null
    }

    private suspend fun recordHistory(
        wakeWord: WakeWord,
        action: WakeAction,
        confidence: Float,
        success: Boolean,
        failureReason: String?
    ) {
        historyEntryDao.insert(
            HistoryEntry(
                wakeWordId = wakeWord.id,
                matchedPhrase = wakeWord.phrase,
                actionType = action.type,
                confidence = confidence,
                triggeredAt = System.currentTimeMillis(),
                success = success,
                failureReason = failureReason
            )
        )
    }

    private fun sendLocationSms(number: String) {
        val hasLoc = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasSms = ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        fun dispatch(msgText: String) {
            try {
                if (hasSms) {
                    getSmsManager().sendTextMessage(number, null, msgText, null, null)
                } else {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                        putExtra("sms_body", msgText)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                    putExtra("sms_body", msgText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }

        if (!hasLoc) {
            dispatch("Emergency alert triggered! Location permission not enabled on device.")
            return
        }

        val isGpsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm?.isLocationEnabled == true
        } else {
            lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                    lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        }

        if (!isGpsEnabled) {
            dispatch("Emergency alert triggered! (Please turn ON Location in phone Quick Settings)")
            return
        }

        var bestCached: android.location.Location? = null
        if (lm != null) {
            val providers = listOf(
                android.location.LocationManager.GPS_PROVIDER,
                android.location.LocationManager.NETWORK_PROVIDER,
                android.location.LocationManager.PASSIVE_PROVIDER
            )
            for (provider in providers) {
                try {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null && (bestCached == null || loc.time > bestCached.time)) {
                        bestCached = loc
                    }
                } catch (_: Exception) {}
            }
        }

        if (bestCached != null) {
            val link = "https://maps.google.com/?q=${bestCached.latitude},${bestCached.longitude}"
            dispatch("Emergency - My location: $link")
            return
        }

        try {
            fusedClient.lastLocation.addOnCompleteListener { task ->
                val loc = if (task.isSuccessful) task.result else null
                if (loc != null) {
                    val link = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                    dispatch("Emergency - My location: $link")
                } else {
                    try {
                        fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                            .addOnCompleteListener { freshTask ->
                                val freshLoc = if (freshTask.isSuccessful) freshTask.result else null
                                if (freshLoc != null) {
                                    val link = "https://maps.google.com/?q=${freshLoc.latitude},${freshLoc.longitude}"
                                    dispatch("Emergency - My location: $link")
                                } else {
                                    var sent = false
                                    if (lm != null && lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                                        try {
                                            val listener = object : android.location.LocationListener {
                                                override fun onLocationChanged(l: android.location.Location) {
                                                    if (!sent) {
                                                        sent = true
                                                        lm.removeUpdates(this)
                                                        val link = "https://maps.google.com/?q=${l.latitude},${l.longitude}"
                                                        dispatch("Emergency - My location: $link")
                                                    }
                                                }
                                                @Deprecated("Deprecated in Java")
                                                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                                                override fun onProviderEnabled(provider: String) {}
                                                override fun onProviderDisabled(provider: String) {}
                                            }
                                            lm.requestSingleUpdate(android.location.LocationManager.NETWORK_PROVIDER, listener, context.mainLooper)
                                        } catch (_: Exception) {}
                                    }
                                    dispatch("Emergency alert triggered! (GPS coordinates acquiring, please ensure clear view or Wi-Fi)")
                                }
                            }
                    } catch (_: Exception) {
                        dispatch("Emergency alert triggered! (GPS coordinates acquiring)")
                    }
                }
            }
        } catch (_: Exception) {
            dispatch("Emergency alert triggered from LowYourTone")
        }
    }

    private fun expandTemplate(template: String): String {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return template.replace("{time}", now).replace("{date}", date)
    }

    private fun getSmsManager(): SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
    } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
    }

    private fun vibrateOnDetection() {
        try {
            val pattern = longArrayOf(0, 200, 100, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator?.vibrate(
                    android.os.VibrationEffect.createWaveform(pattern, -1)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(
                    android.os.VibrationEffect.createWaveform(pattern, -1)
                )
            }
        } catch (_: Exception) {}
    }

    private fun showDetectionNotification(wakeWord: WakeWord, action: WakeAction) {
        try {
            val intent = Intent(context, dev.anupam.lowyourtone.MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, "detection_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Detected: ${wakeWord.phrase.uppercase()}")
                .setContentText("Running: ${action.label}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(pendingIntent)
                .build()
            notificationManager.cancel(43)
            notificationManager.notify(43, notification)
        } catch (_: Exception) {}
    }

    private fun showConfirmationNotification(wakeWord: WakeWord, action: WakeAction, confidence: Float) {
        val executeIntent = Intent(context, ConfirmActionReceiver::class.java).apply {
            this.action = "dev.anupam.lowyourtone.CONFIRM_ACTION"
            putExtra("wake_word_id", wakeWord.id)
            putExtra("action_id", action.id)
            putExtra("confidence", confidence)
        }
        val executePending = android.app.PendingIntent.getBroadcast(
            context, wakeWord.id.hashCode(), executeIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "detection_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Confirm: ${wakeWord.phrase}")
            .setContentText("Tap to execute ${action.label}")
            .setContentIntent(executePending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(30000)
            .build()
        notificationManager.notify(wakeWord.id.hashCode(), notification)
    }

    private fun showEmergencyFailureNotification(reason: String) {
        val notification = NotificationCompat.Builder(context, "emergency_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("EMERGENCY ACTION FAILED")
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showActionErrorNotification(actionName: String, reason: String) {
        val notification = NotificationCompat.Builder(context, "error_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Action failed: $actionName")
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(actionName.hashCode(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel("detection_channel", "Detection Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
            notificationManager.createNotificationChannel(
                NotificationChannel("emergency_channel", "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
            notificationManager.createNotificationChannel(
                NotificationChannel("error_channel", "Errors & Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun parseParams(json: String): Map<String, Any> {
        if (json.isBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
}
