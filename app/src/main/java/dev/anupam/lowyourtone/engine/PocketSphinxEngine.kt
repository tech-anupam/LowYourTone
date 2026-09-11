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

class PocketSphinxEngine : WakeWordEngine, RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var callback: WakeWordCallback? = null
    private val handlerThread = HandlerThread("PocketSphinxThread").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private var context: Context? = null
    @Volatile private var listening = false
    @Volatile private var currentKeywords: List<KeywordEntry> = emptyList()
    private var syncAssetsDir: File? = null

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
                .setKeywordThreshold(1e-25f)
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
                    val th = if (words <= 1) "1e-25" else if (words == 2) "1e-35" else "1e-45"
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
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    override fun stopListening() {
        handler.post {
            try {
                listening = false
                recognizer?.stop()
                callback?.onListeningStopped()
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    override fun shutdown() {
        handler.post {
            recognizer?.cancel()
            recognizer?.shutdown()
            recognizer = null
            context = null
            callback = null
            listening = false
            currentKeywords = emptyList()
            handlerThread.quitSafely()
        }
    }

    override fun isListening(): Boolean = listening

    override fun onBeginningOfSpeech() {}
    override fun onEndOfSpeech() {}

    override fun onPartialResult(hypothesis: Hypothesis?) {
        hypothesis ?: return
        val text = hypothesis.hypstr?.trim()?.lowercase() ?: return
        if (text.isEmpty()) return

        val matchedEntry = currentKeywords.find { entry ->
            val target = entry.phrase.trim().lowercase()
            text == target || text.contains(target) || (target.contains(" ") && target.split(" ").all { text.contains(it) })
        }

        if (matchedEntry != null) {
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
    }

    override fun onResult(hypothesis: Hypothesis?) {
        hypothesis ?: return
        val text = hypothesis.hypstr?.trim()?.lowercase() ?: return
        if (text.isEmpty()) return

        val matchedEntry = currentKeywords.find { entry ->
            val target = entry.phrase.trim().lowercase()
            text == target || text.contains(target) || (target.contains(" ") && target.split(" ").all { text.contains(it) })
        }

        if (matchedEntry != null) {
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
    }

    override fun onError(error: Exception?) {
        error?.let { callback?.onError(it) }
    }

    override fun onTimeout() {}
}
