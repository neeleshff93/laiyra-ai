package com.laiyra.ai

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.laiyra.ai.overlay.FloatingService
import com.laiyra.ai.ui.screen.DarkBackground
import com.laiyra.ai.ui.screen.HomeScreen
import com.laiyra.ai.ui.screen.SplashScreen
import com.laiyra.ai.voice.VoskService
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Overlay screen se wapas aaya — dono services start
        startFloatingService()
        startVoskService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ① Runtime permissions
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())

        // ② Overlay permission — sirf pehli baar
        val prefs = getSharedPreferences("laiyra_prefs", MODE_PRIVATE)
        val asked = prefs.getBoolean("overlay_asked", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this) &&
            !asked
        ) {
            prefs.edit().putBoolean("overlay_asked", true).apply()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayLauncher.launch(intent)
        } else {
            // Permission already thi ya user ne mana kiya
            startFloatingService()
            startVoskService()
        }

        // ③ UI render
        setContent {
            MaterialTheme {
                Surface(color = DarkBackground) {
                    AppRoot()
                }
            }
        }
    }

    private fun startFloatingService() {
        try {
            val intent = Intent(this, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            android.util.Log.d("Laiyra", "FloatingService started")
        } catch (e: Exception) {
            android.util.Log.e("Laiyra", "FloatingService failed: ${e.message}")
        }
    }

    private fun startVoskService() {
        try {
            val intent = Intent(this, VoskService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            android.util.Log.d("Laiyra", "VoskService started")
        } catch (e: Exception) {
            android.util.Log.e("Laiyra", "VoskService failed: ${e.message}")
        }
    }
}

@Composable
private fun AppRoot() {

    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }

    // Splash Voice — English Female
    val splashTts = remember {
        var tts: TextToSpeech? = null
        var ready = false

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.1f)
                ready = true
            }
        }

        object {
            fun speak(text: String) {
                if (ready) {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "splash")
                }
            }
            fun shutdown() {
                try {
                    tts?.stop()
                    tts?.shutdown()
                } catch (_: Exception) {}
                tts = null
            }
        }
    }

    if (showSplash) {
        SplashScreen(
            onFinished = {
                splashTts.shutdown()
                showSplash = false
            },
            speakWelcome = { splashTts.speak(it) }
        )
    } else {
        HomeScreen()
    }
}

