package com.laiyra.ai.ui.screen

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laiyra.ai.voice.VoiceSessionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LaiyraViewModel(app: Application) : AndroidViewModel(app) {

    private val tts = TextToSpeechEngine(app)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _speechText = MutableStateFlow("")
    val speechText: StateFlow<String> = _speechText

    private val _liveAiReply = MutableStateFlow("")
    val liveAiReply: StateFlow<String> = _liveAiReply

    private var sessionRunning = false

    init {
        // 🎙️ Intro greeting — jab app pehli baar khule
        viewModelScope.launch {
            delay(700)
            val intro = listOf(
                "Hello Master! Main Laiyra hoon. Mic tap karke baat shuru karein.",
                "Namaste Master! Mic dabaiye aur boliye.",
                "Hi Sir! Mic tap kariye, session start karein.",
                "Arre, aa gaye aap! Mic tap kariye.",
                "Hey Master! Mic button dabaiye."
            ).random()

            ConversationMemory.addAi(intro)
            tts.speak(intro)
        }
    }

    /**
     * Mic button toggle:
     *  - Agar session chal raha hai → STOP
     *  - Agar band hai → START
     */
    fun toggleMic() {
        val ctx = getApplication<Application>()

        if (sessionRunning) {
            // 🛑 SESSION STOP
            sessionRunning = false
            _isListening.value = false

            val stopIntent = Intent(ctx, VoiceSessionService::class.java).apply {
                action = "STOP"
            }
            try {
                ctx.startService(stopIntent)
            } catch (_: Exception) {}
        } else {
            // ✅ SESSION START
            sessionRunning = true
            _isListening.value = true

            val startIntent = Intent(ctx, VoiceSessionService::class.java).apply {
                action = "START"
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(startIntent)
                } else {
                    ctx.startService(startIntent)
                }
            } catch (_: Exception) {
                sessionRunning = false
                _isListening.value = false
            }
        }
    }

    override fun onCleared() {
        // ViewModel clear → session band nahi karenge
        // (session service alag chalti hai)
        try {
            tts.shutdown()
        } catch (_: Exception) {}
    }
}