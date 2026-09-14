package dev.anupam.lowyourtone.engine

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import edu.cmu.pocketsphinx.Assets
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class PocketSphinxEngine : WakeWordEngine, RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var callback: WakeWordCallback? = null
    private val handlerThread = HandlerThread("PocketSphinxThread").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private var context: Context? = null
    @Volatile private var listening = false
    @Volatile private var currentKeywords: List<KeywordEntry> = emptyList()
    private var syncAssetsDir: File? = null

    private val lastTriggerTimes = ConcurrentHashMap<String, Long>()
    private val DEBOUNCE_MS = 1500L

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (listening && recognizer != null) {
                try {
                    recognizer?.stop()
                    recognizer?.startListening("KWS_SEARCH")
                } catch (e: Exception) {
                    attemptRestart()
                }
            }
            if (listening) {
                handler.postDelayed(this, 30_000L)
            }
        }
    }

    override fun initialize(context: Context, assetsDir: File) {
        this.context = context
        handler.post {
            try {
                val syncedDir = Assets(context).syncAssets()
                this.syncAssetsDir = syncedDir
                setupRecognizer(syncedDir)
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    private fun setupRecognizer(syncDir: File) {
        try {
            recognizer?.cancel()
            recognizer?.shutdown()
            recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(syncDir, "models/en-us-ptm"))
                .setDictionary(File(syncDir, "models/lm/words.dic"))
                .setKeywordThreshold(1e-30f)
                .recognizer.apply {
                    addListener(this@PocketSphinxEngine)
                }
            setupSearch()
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    private fun setupSearch() {
        val ctx = context ?: return
        val rec = recognizer ?: return
        if (currentKeywords.isEmpty()) return

        try {
            if (listening) rec.stop()
            val kwlistFile = File(ctx.filesDir, "keywords.kwlist")
            FileOutputStream(kwlistFile).bufferedWriter().use { writer ->
                currentKeywords.forEach { entry ->
                    val p = entry.phrase.trim().lowercase()
                    val words = p.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    val th = thresholdForSensitivity(entry.threshold, words)
                    writer.write("$p /$th/\n")
                }
            }
            rec.addKeywordSearch("KWS_SEARCH", kwlistFile)
            if (listening) {
                rec.startListening("KWS_SEARCH")
            }
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    private fun thresholdForSensitivity(sensitivity: Float, wordCount: Int): String {
        // A value nearer to 1 is stricter in PocketSphinx keyword spotting.
        // This minimum protects against legacy, overly-sensitive trigger values.
        val safeMinimum = when {
            wordCount <= 1 -> 1e-15f
            wordCount == 2 -> 1e-20f
            else -> 1e-25f
        }
        val configured = sensitivity.takeIf { it > 0f && it <= 1f } ?: safeMinimum
        val strictThreshold = maxOf(configured, safeMinimum)
        val exponent = kotlin.math.ceil(kotlin.math.log10(strictThreshold.toDouble())).toInt()
        return "1e$exponent"
    }

    override fun loadKeywords(keywords: List<KeywordEntry>) {
        this.currentKeywords = keywords
        handler.post {
            val dir = syncAssetsDir
            if (dir != null) {
                setupRecognizer(dir)
            } else {
                setupSearch()
            }
        }
    }

    override fun startListening(callback: WakeWordCallback) {
        this.callback = callback
        handler.post {
            try {
                listening = true
                if (currentKeywords.isNotEmpty()) {
                    recognizer?.let { rec ->
                        rec.stop()
                        rec.startListening("KWS_SEARCH")
                        callback.onListeningStarted()
                    }
                }
                handler.removeCallbacks(watchdogRunnable)
                handler.postDelayed(watchdogRunnable, 30_000L)
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    override fun stopListening() {
        handler.post {
            try {
                listening = false
                handler.removeCallbacks(watchdogRunnable)
                recognizer?.stop()
                callback?.onListeningStopped()
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    fun pauseListening() {
        handler.post {
            try {
                recognizer?.stop()
            } catch (_: Exception) {}
        }
    }

    fun resumeListening() {
        handler.post {
            try {
                if (listening && currentKeywords.isNotEmpty()) {
                    recognizer?.startListening("KWS_SEARCH")
                }
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    override fun shutdown() {
        handler.post {
            handler.removeCallbacks(watchdogRunnable)
            recognizer?.cancel()
            recognizer?.shutdown()
            recognizer = null
            context = null
            callback = null
            listening = false
            currentKeywords = emptyList()
            lastTriggerTimes.clear()
            handlerThread.quitSafely()
        }
    }

    override fun isListening(): Boolean = listening

    override fun onBeginningOfSpeech() {}
    override fun onEndOfSpeech() {}

    override fun onPartialResult(hypothesis: Hypothesis?) {
        try {
            hypothesis ?: return
            val text = hypothesis.hypstr?.trim()?.lowercase() ?: return
            if (text.isEmpty()) return

            val matchedEntry = findExactMatch(text)

            if (matchedEntry != null) {
                val now = System.currentTimeMillis()
                val phraseKey = matchedEntry.phrase.trim().lowercase()
                val lastTime = lastTriggerTimes[phraseKey] ?: 0L
                if (now - lastTime < DEBOUNCE_MS) return

                lastTriggerTimes[phraseKey] = now

                handler.post {
                    try {
                        recognizer?.stop()
                        callback?.onWakeWordDetected(matchedEntry.phrase, 1.0f)
                        recognizer?.startListening("KWS_SEARCH")
                    } catch (e: Exception) {
                        callback?.onError(e)
                    }
                }
            }
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    override fun onResult(hypothesis: Hypothesis?) {
        if (!listening) return
        handler.post {
            try {
                recognizer?.startListening("KWS_SEARCH")
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    override fun onError(error: Exception?) {
        error?.let { callback?.onError(it) }
        attemptRestart()
    }

    override fun onTimeout() {
        if (listening) {
            handler.post {
                try {
                    recognizer?.startListening("KWS_SEARCH")
                } catch (e: Exception) {
                    callback?.onError(e)
                }
            }
        }
    }

    private fun findExactMatch(text: String): KeywordEntry? {
        val heard = normalizePhrase(text)
        return currentKeywords.find { entry ->
            // Reject partial matches embedded in normal conversation.
            normalizePhrase(entry.phrase) == heard
        }
    }

    private fun normalizePhrase(value: String): String = value
        .lowercase()
        .replace(Regex("[^a-z0-9']+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun attemptRestart() {
        if (!listening) return
        handler.postDelayed({
            val dir = syncAssetsDir ?: return@postDelayed
            try {
                setupRecognizer(dir)
                if (currentKeywords.isNotEmpty()) {
                    recognizer?.startListening("KWS_SEARCH")
                }
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }, 2000L)
    }
}
