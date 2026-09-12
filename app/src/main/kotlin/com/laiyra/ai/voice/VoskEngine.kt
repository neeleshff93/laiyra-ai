package com.laiyra.ai.voice

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

class VoskEngine(private val context: Context) {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    var isReady = false
        private set

    var onWakeWord: (() -> Unit)? = null
    var onPartialResult: ((String) -> Unit)? = null
    var onFinalResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onDebug: ((String) -> Unit)? = null

    private val wakeWords = listOf(
        "laiyra", "laira", "layra", "laiyara", "leyra",
        "lirra", "lara", "liyara", "laiyraa", "laiyaraa",
        "hey laiyra", "hey laira", "o laiyra", "o laira",
        "hi laiyra", "are laiyra", "arre laiyra"
    )

    private val modelPaths = listOf(
        "/sdcard/Download/vosk-model-small-hi-0.22",
        "/storage/emulated/0/Download/vosk-model-small-hi-0.22",
        "/sdcard/Downloads/vosk-model-small-hi-0.22",
        "/storage/emulated/0/Downloads/vosk-model-small-hi-0.22"
    )

    fun initialize() {
        try {
            val modelDir = findModel()
            if (modelDir == null) {
                val msg = "Model not found in Downloads"
                onError?.invoke(msg)
                onDebug?.invoke("❌ $msg")
                Log.e("Vosk", msg)
                return
            }

            onDebug?.invoke("📂 Loading model: ${modelDir.name}")
            Log.d("Vosk", "Loading model from: ${modelDir.absolutePath}")

            model = Model(modelDir.absolutePath)
            isReady = true
            onDebug?.invoke("✅ Model loaded!")
            Log.d("Vosk", "Model loaded successfully")

        } catch (e: Throwable) {
            val msg = "Model load failed: ${e.message}"
            onError?.invoke(msg)
            onDebug?.invoke("❌ $msg")
            Log.e("Vosk", msg, e)
        }
    }

    private fun findModel(): File? {
        for (path in modelPaths) {
            val f = File(path)
            if (f.exists() && f.isDirectory) {
                Log.d("Vosk", "Found model at: $path")
                return f
            }
        }
        return null
    }

    fun startListening() {
        val m = model
        if (m == null) {
            onError?.invoke("Model not loaded")
            return
        }

        try {
            val rec = Recognizer(m, 16000.0f)
            val listener = object : RecognitionListener {

                override fun onPartialResult(hypothesis: String?) {
                    val text = extractText(hypothesis) ?: return
                    if (text.isBlank()) return

                    Log.d("Vosk", "Partial: $text")
                    onPartialResult?.invoke(text)

                    val lower = text.lowercase()
                    if (wakeWords.any { lower.contains(it) }) {
                        Log.d("Vosk", "WAKE WORD HIT: $text")
                        onDebug?.invoke("🎯 Wake: $text")
                        onWakeWord?.invoke()
                    }
                }

                override fun onResult(hypothesis: String?) {
                    val text = extractText(hypothesis) ?: return
                    if (text.isBlank()) return

                    Log.d("Vosk", "Result: $text")
                    onDebug?.invoke("📝 Final: $text")

                    val lower = text.lowercase()
                    if (wakeWords.any { lower.contains(it) }) {
                        Log.d("Vosk", "WAKE WORD HIT (final): $text")
                        onWakeWord?.invoke()
                    } else {
                        onFinalResult?.invoke(text)
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    val text = extractText(hypothesis) ?: return
                    if (text.isBlank()) return

                    Log.d("Vosk", "FinalResult: $text")
                    val lower = text.lowercase()
                    if (wakeWords.any { lower.contains(it) }) {
                        onWakeWord?.invoke()
                    } else {
                        onFinalResult?.invoke(text)
                    }
                }

                override fun onError(exception: Exception?) {
                    val msg = exception?.message ?: "Unknown error"
                    Log.e("Vosk", "Listener error: $msg")
                    onDebug?.invoke("⚠️ Err: $msg")
                    onError?.invoke(msg)
                }

                override fun onTimeout() {
                    Log.d("Vosk", "Timeout")
                }
            }

            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(listener)
            onDebug?.invoke("🎤 Listening started")
            Log.d("Vosk", "Listening started")

        } catch (e: Throwable) {
            val msg = "startListening failed: ${e.message}"
            Log.e("Vosk", msg, e)
            onDebug?.invoke("❌ $msg")
            onError?.invoke(msg)
        }
    }

    private fun extractText(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            JSONObject(json).optString("text", "").trim()
        } catch (_: Exception) {
            null
        }
    }

    fun stopListening() {
        try { speechService?.stop() } catch (_: Throwable) {}
        speechService = null
    }

    fun destroy() {
        stopListening()
        try { model?.close() } catch (_: Throwable) {}
        model = null
        isReady = false
    }
}