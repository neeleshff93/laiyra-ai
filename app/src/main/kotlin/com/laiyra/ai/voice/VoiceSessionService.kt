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
import android.util.Log
import com.laiyra.ai.MainActivity
import com.laiyra.ai.ui.screen.BrainConfig
import com.laiyra.ai.ui.screen.ConversationMemory
import com.laiyra.ai.ui.screen.SmartActionDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceSessionService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var restartJob: Job? = null

    private var isSessionActive = false
    private var isSpeaking = false
    private var isListening = false
    private var isRestarting = false

    private lateinit var dispatcher: SmartActionDispatcher

    // Stop commands
    private val stopCommands = listOf(
        "laiyra deactivate", "laiyra band", "laiyra stop",
        "deactivate", "end session", "stop session", "call end",
        "session end", "band karo laiyra", "laiyra chup"
    )

    // Start commands (agr session off hai)
    private val startCommands = listOf(
        "laiyra activate", "laiyra start", "hey laiyra",
        "activate laiyra", "laiyra start karo", "start laiyra"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("Laiyra", "VoiceSessionService onCreate")
        dispatcher = SmartActionDispatcher(this)
        startForegroundNotification()
        initTts()
        initRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("Laiyra", "onStartCommand action=$action")

        when (action) {
            "START" -> {
                if (!isSessionActive) {
                    isSessionActive = true
                    speak("Session active. Boliye Master.")
                }
            }
            "STOP" -> {
                endSession()
            }
            else -> {
                if (!isSessionActive) {
                    isSessionActive = true
                }
            }
        }

        return START_STICKY
    }

    // ============================================================
    // 🔔 NOTIFICATION
    // ============================================================
    private fun startForegroundNotification() {
        val channelId = "laiyra_session"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Laiyra Session",
                NotificationManager.IMPORTANCE_LOW
            )
            ch.setShowBadge(false)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }

        // Open app
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // End session
        val stopIntent = Intent(this, VoiceSessionService::class.java).apply {
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
            .setContentTitle("Laiyra — Session Active")
            .setContentText("Baat karo, sun rahi hoon...")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(openPi)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Session",
                stopPi
            )
            .setOngoing(true)
            .build()

        startForeground(2002, notif)
    }

    // ============================================================
    // 🔊 TTS
    // ============================================================
    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
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
                            // Auto restart listening
                            if (isSessionActive) {
                                scheduleRestart(600)
                            }
                        }
                        @Deprecated("Deprecated")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            if (isSessionActive) {
                                scheduleRestart(600)
                            }
                        }
                    }
                )
                ttsReady = true
            }
        }
    }

    // ============================================================
    // 🎤 RECOGNIZER
    // ============================================================
    private fun initRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(listener)
        }
    }

    private fun startListening() {
        if (!isSessionActive || isSpeaking || isRestarting) return
        if (recognizer == null) initRecognizer()

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
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1200L
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

    /** Safe restart — loop break ke liye 1.5 sec gap */
    private fun scheduleRestart(delayMs: Long = 1500) {
        if (!isSessionActive) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            if (isSessionActive && !isSpeaking && !isListening) {
                startListening()
            }
        }
    }

    // ============================================================
    // 🎧 LISTENER
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

            // Silent restart for all errors
            if (isSessionActive) {
                scheduleRestart(2000)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

            if (text.isNullOrBlank()) {
                scheduleRestart(1000)
                return
            }

            handleUserText(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Not used in session mode
        }
    }

    // ============================================================
    // 🧠 HANDLE USER INPUT
    // ============================================================
    private fun handleUserText(text: String) {
        val t = text.lowercase().trim()

        // Stop command check
        if (stopCommands.any { t.contains(it) }) {
            speak("Session band kar rahi hoon.")
            scope.launch {
                delay(1500)
                endSession()
            }
            return
        }

        ConversationMemory.addUser(text)

        // Local commands
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

    // ============================================================
    // 🔊 SPEAK
    // ============================================================
    private fun speak(text: String) {
        if (!ttsReady) {
            scheduleRestart(500)
            return
        }

        // Emoji strip
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
            scheduleRestart(500)
            return
        }

        isSpeaking = true
        stopListening()
        tts?.speak(
            clean,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "laiyra_${System.currentTimeMillis()}"
        )
    }

    // ============================================================
    // 🔴 END SESSION
    // ============================================================
    private fun endSession() {
        Log.d("Laiyra", "Session ending")
        isSessionActive = false
        restartJob?.cancel()

        try { tts?.stop() } catch (_: Exception) {}
        try { recognizer?.stopListening() } catch (_: Exception) {}

        stopSelf()
    }

    override fun onDestroy() {
        Log.d("Laiyra", "VoiceSessionService destroyed")
        isSessionActive = false
        restartJob?.cancel()

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
