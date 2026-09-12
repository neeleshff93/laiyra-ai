package com.laiyra.ai.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.laiyra.ai.MainActivity
import com.laiyra.ai.ui.screen.BrainConfig
import com.laiyra.ai.ui.screen.ConversationMemory
import com.laiyra.ai.ui.screen.SmartActionDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * CAL Jaisa Continuous Voice Service.
 * Ek baar start → continuous conversation.
 * Mic button se band karo.
 */
class ContinuousCallService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State
    private var isActive = false
    private var isSpeaking = false
    private var isListening = false
    private var restarting = false

    private lateinit var dispatcher: SmartActionDispatcher

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        dispatcher = SmartActionDispatcher(this)
        startForegroundNotif()
        initTts()
        initRecognizer()
        isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isListening && !isSpeaking) {
            startListening()
        }
        return START_STICKY
    }

    // ============================================================
    // 📌 FOREGROUND NOTIFICATION
    // ============================================================
    private fun startForegroundNotif() {
        val channelId = "laiyra_call"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Laiyra Call",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }

        // Tap → open app
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop action
        val stopIntent = Intent(this, ContinuousCallService::class.java).apply {
            action = "STOP"
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notif = builder
            .setContentTitle("Laiyra — Voice Call Active")
            .setContentText("Baat karo, sun rahi hoon...")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End", stopPi)
            .setOngoing(true)
            .build()

        startForeground(2001, notif)
    }

    // ============================================================
    // 🔊 TTS
    // ============================================================
    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Hindi female
                val r = tts?.setLanguage(Locale("hi", "IN"))
                if (r == TextToSpeech.LANG_MISSING_DATA ||
                    r == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts?.language = Locale.US
                }
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.15f)

                tts?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                        }
                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                            // 🔄 Auto restart mic after TTS done
                            if (isActive) {
                                scope.launch {
                                    delay(500)  // small gap
                                    if (isActive && !isListening) {
                                        startListening()
                                    }
                                }
                            }
                        }
                        @Deprecated("Deprecated")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            if (isActive) {
                                scope.launch {
                                    delay(500)
                                    if (isActive && !isListening) {
                                        startListening()
                                    }
                                }
                            }
                        }
                    }
                )
                ttsReady = true
            }
        }
    }

    // ============================================================
    // 🎤 RECOGNIZER — Continuous listening
    // ============================================================
    private fun initRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(listener)
        }
    }

    private fun startListening() {
        if (!isActive || isSpeaking || restarting) return
        if (recognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(
                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                arrayListOf("en-IN", "en-US")
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Long silence so user can speak freely
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                3000L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                3000L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1500L
            )
        }

        try {
            isListening = true
            recognizer?.cancel()
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            isListening = false
        }
    }

    private fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
    }

    /** Silent restart — loop se bachne ke liye, thoda delay */
    private fun restartListeningSafely() {
        if (restarting || !isActive) return
        restarting = true

        scope.launch {
            delay(1500)  // ⏱️ 1.5 sec wait — loop break
            restarting = false
            if (isActive && !isSpeaking && !isListening) {
                startListening()
            }
        }
    }

    // ============================================================
    // 🎧 RECOGNITION LISTENER
    // ============================================================
    private val listener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            isListening = false

            // 🛑 Silence / no match → silently restart
            if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                error == SpeechRecognizer.ERROR_NO_MATCH
            ) {
                restartListeningSafely()
                return
            }

            // Other errors — restart with delay
            restartListeningSafely()
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            if (text.isNullOrBlank()) {
                restartListeningSafely()
                return
            }

            handleUserText(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Optional: partial text dikha sakte ho
        }
    }

    // ============================================================
    // 🧠 HANDLE USER TEXT
    // ============================================================
    private fun handleUserText(text: String) {
        // Save to memory
        ConversationMemory.addUser(text)

        // Check interrupt words
        if (isInterrupt(text)) {
            tts?.stop()
            isSpeaking = false
            // Restart listening
            scope.launch {
                delay(300)
                restartListeningSafely()
            }
            return
        }

        // Local command first
        val localReply = dispatcher.tryHandle(text)
        if (localReply != null) {
            ConversationMemory.addAi(localReply)
            speak(localReply)
            return
        }

        // Cloud brain
        scope.launch {
            val reply = BrainConfig.build().reply(text, ConversationMemory.history())
            ConversationMemory.addAi(reply)
            speak(reply)
        }
    }

    private fun isInterrupt(text: String): Boolean {
        val t = text.lowercase().trim()
        val words = listOf(
            "chup", "chup karo", "stop", "stop karo",
            "ruko", "ruk jao", "bas", "bas karo",
            "quiet", "silence", "band karo"
        )
        return words.any { t == it || t.contains(it) }
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        // strip emoji
        val clean = text
            .replace(
                Regex(
                    "[\uD83C\uDF00-\uD83D\uDDFF]" +
                    "|[\uD83D\uDE00-\uD83D\uDE4F]" +
                    "|[\uD83C\uDDE6-\uD83C\uDDFF]" +
                    "|[\uD83D\uDC00-\uD83D\uDDFF]" +
                    "|[\u2600-\u27BF]" +
                    "|[\uFE0F]" +
                    "|[\u200D]"
                ),
                ""
            )
            .trim()

        if (clean.isBlank()) {
            restartListeningSafely()
            return
        }

        isSpeaking = true
        stopListening()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null,
            "laiyra_${System.currentTimeMillis()}")
    }

    override fun onDestroy() {
        isActive = false
        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        scope.cancel()
        super.onDestroy()
    }
}

