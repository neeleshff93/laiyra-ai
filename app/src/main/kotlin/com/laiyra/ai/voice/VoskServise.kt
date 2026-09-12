package com.laiyra.ai.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.laiyra.ai.MainActivity
import com.laiyra.ai.ui.screen.BrainConfig
import com.laiyra.ai.ui.screen.ConversationMemory
import com.laiyra.ai.ui.screen.SmartActionDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class VoskService : Service() {

    private var vosk: VoskEngine? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var dispatcher: SmartActionDispatcher? = null

    private var wakeWordDetected = false
    private var isSpeaking = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        try {
            Log.d("Vosk", "onCreate started")

            startForegroundNotification()
            dispatcher = SmartActionDispatcher(this)
            initTts()
            initVosk()

            Log.d("Vosk", "onCreate completed")

        } catch (e: Throwable) {
            Log.e("Vosk", "onCreate failed: ${e.message}", e)
            // Show error in chat
            addDebugMessage("❌ Service fail: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent?.action == "STOP") {
                stopSelf()
                return START_NOT_STICKY
            }
        } catch (_: Throwable) {}
        return START_STICKY
    }

    /** Chat me debug message add karo */
    private fun addDebugMessage(msg: String) {
        try {
            ConversationMemory.addAi(msg)
        } catch (_: Throwable) {}
    }

    private fun startForegroundNotification() {
        val channelId = "laiyra_vosk"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Laiyra Wake Word",
                NotificationManager.IMPORTANCE_LOW
            )
            ch.setShowBadge(false)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, VoskService::class.java).apply {
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
            .setContentTitle("Laiyra Sun Rahi Hai")
            .setContentText("\"Laiyra\" bolo...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPi)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPi
            )
            .setOngoing(true)
            .build()

        startForeground(3001, notif)
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(this) { status ->
                try {
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
                                    try { vosk?.stopListening() } catch (_: Throwable) {}
                                }
                                override fun onDone(utteranceId: String?) {
                                    isSpeaking = false
                                    wakeWordDetected = false
                                    try { vosk?.startListening() } catch (_: Throwable) {}
                                }
                                @Deprecated("Deprecated")
                                override fun onError(utteranceId: String?) {
                                    isSpeaking = false
                                    wakeWordDetected = false
                                    try { vosk?.startListening() } catch (_: Throwable) {}
                                }
                            }
                        )
                        ttsReady = true
                    }
                } catch (e: Throwable) {
                    Log.e("Vosk", "TTS init callback failed: ${e.message}", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("Vosk", "TTS init failed: ${e.message}", e)
        }
    }

    private fun initVosk() {
        try {
            vosk = VoskEngine(this).apply {
                onError = { msg ->
                    Log.e("Vosk", "Engine error: $msg")
                    addDebugMessage("⚠️ $msg")
                }

                onDebug = { msg ->
                    Log.d("Vosk", "Debug: $msg")
                    // Har debug message chat me bhejo
                    addDebugMessage(msg)
                }

                onWakeWord = {
                    try {
                        if (!wakeWordDetected) {
                            wakeWordDetected = true
                            Log.d("Vosk", "Wake word detected!")
                            addDebugMessage("🎯 Laiyra activated!")
                            speak("Haan Master, boliye?")
                        }
                    } catch (e: Throwable) {
                        Log.e("Vosk", "WakeWord handler failed: ${e.message}", e)
                    }
                }

                onFinalResult = { text ->
                    try {
                        if (wakeWordDetected && !isSpeaking) {
                            addDebugMessage("🧠 Processing: $text")
                            handleCommand(text)
                        }
                    } catch (e: Throwable) {
                        Log.e("Vosk", "Final result handler failed: ${e.message}", e)
                    }
                }

                onPartialResult = { text ->
                    // Har partial chat me bhejo
                    if (text.length >= 2) {
                        Log.d("Vosk", "Partial (chat): $text")
                    }
                }
            }

            vosk?.initialize()

            if (vosk?.isReady == true) {
                vosk?.startListening()
                Log.d("Vosk", "Listening started")
            } else {
                Log.e("Vosk", "Vosk not ready")
            }
        } catch (e: Throwable) {
            Log.e("Vosk", "initVosk failed: ${e.message}", e)
            addDebugMessage("❌ Vosk init failed: ${e.message}")
        }
    }

    private fun handleCommand(text: String) {
        try {
            Log.d("Vosk", "Command: $text")
            ConversationMemory.addUser(text)

            val localReply = dispatcher?.tryHandle(text)
            if (localReply != null) {
                ConversationMemory.addAi(localReply)
                speak(localReply)
                return
            }

            scope.launch {
                try {
                    val reply = BrainConfig.build().reply(text, ConversationMemory.history())
                    ConversationMemory.addAi(reply)
                    speak(reply)
                } catch (e: Throwable) {
                    Log.e("Vosk", "Brain reply failed: ${e.message}", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("Vosk", "handleCommand failed: ${e.message}", e)
        }
    }

    private fun speak(text: String) {
        try {
            if (!ttsReady) {
                wakeWordDetected = false
                try { vosk?.startListening() } catch (_: Throwable) {}
                return
            }

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
                wakeWordDetected = false
                try { vosk?.startListening() } catch (_: Throwable) {}
                return
            }

            isSpeaking = true
            tts?.speak(
                clean,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "laiyra_${System.currentTimeMillis()}"
            )
        } catch (e: Throwable) {
            Log.e("Vosk", "speak failed: ${e.message}", e)
            isSpeaking = false
            wakeWordDetected = false
        }
    }

    override fun onDestroy() {
        Log.d("Vosk", "VoskService destroyed")
        try { tts?.stop() } catch (_: Throwable) {}
        try { tts?.shutdown() } catch (_: Throwable) {}
        try { vosk?.destroy() } catch (_: Throwable) {}
        try { scope.cancel() } catch (_: Throwable) {}
        super.onDestroy()
    }
}