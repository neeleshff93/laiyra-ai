package com.laiyra.ai.ui.screen

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    speakWelcome: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // Animation phases
    var gateOpen by remember { mutableStateOf(false) }
    var coreVisible by remember { mutableStateOf(false) }
    var scanActive by remember { mutableStateOf(false) }
    var onlineVisible by remember { mutableStateOf(false) }

    // Gate slide
    val gateProgress by animateFloatAsState(
        targetValue = if (gateOpen) 1f else 0f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "gate"
    )

    // Core reveal (rings + particles + text)
    val coreAlpha by animateFloatAsState(
        targetValue = if (coreVisible) 1f else 0f,
        animationSpec = tween(900),
        label = "coreAlpha"
    )
    val coreScale by animateFloatAsState(
        targetValue = if (coreVisible) 1f else 0.6f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "coreScale"
    )

    // Scan line sweep
    val scanProgress by animateFloatAsState(
        targetValue = if (scanActive) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "scan"
    )

    // "ONLINE" text
    val onlineAlpha by animateFloatAsState(
        targetValue = if (onlineVisible) 1f else 0f,
        animationSpec = tween(700),
        label = "online"
    )

    // Slow rotation for outer ring
    val infinite = rememberInfiniteTransition(label = "core")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    // Particles (fixed random positions)
    val particles = remember {
        List(140) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val radius = Random.nextFloat() * 165f
            SplashParticle(
                angle = angle,
                radius = radius,
                size = Random.nextFloat() * 1.6f + 0.5f,
                alpha = Random.nextFloat() * 0.6f + 0.4f
            )
        }
    }

    LaunchedEffect(Unit) {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 75)

        try {
            // ─── Phase 1: Mechanical beep beep ───
            delay(250)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 110)
            delay(200)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 110)
            delay(220)

            // ─── Phase 2: Long hum + gates open ───
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
            gateOpen = true
            delay(1200)

            // ─── Phase 3: Core reveal (rings + particles + LAIYRA AI) ───
            coreVisible = true
            delay(900)

            // ─── Phase 4: Scan line sweep ───
            scanActive = true

            // 🔊 Voice: Welcome
            speakWelcome("Welcome to Laiyra, Artificial Intelligence")

            delay(850)

            // ─── Phase 5: ONLINE text ───
            onlineVisible = true

            // Wait for voice + animation to finish
            delay(3400)

        } finally {
            tone.release()
        }

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // =====================================================
        // CENTER CORE (rings + particles + text)
        // =====================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(coreAlpha)
                .scale(coreScale),
            contentAlignment = Alignment.Center
        ) {
            // ---- Rings + particles canvas ----
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseR = size.minDimension * 0.42f

                // 4 concentric rings (outer → inner, thinner alpha)
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.85f),
                    radius = baseR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.55f),
                    radius = baseR * 0.82f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.4f)
                )
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.35f),
                    radius = baseR * 0.62f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8f)
                )
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.22f),
                    radius = baseR * 0.42f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4f)
                )

                // Particles
                particles.forEach { p ->
                    val x = cx + cos(p.angle) * p.radius
                    val y = cy + sin(p.angle) * p.radius
                    drawCircle(
                        color = NeonCyan.copy(alpha = p.alpha * 0.75f),
                        radius = p.size,
                        center = Offset(x, y)
                    )
                }
            }

            // ---- Center text ----
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "LAIYRA",
                        color = Color.White,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " AI",
                        color = NeonCyan,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Y O U R   P E R S O N A L   A I   A S S I S T A N T",
                    color = NeonCyan.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(20.dp))

                // "LAIYRA AI ONLINE..." text (with alpha animation)
                Text(
                    text = "LAIYRA AI ONLINE...",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(onlineAlpha)
                )
            }
        }

        // =====================================================
        // HORIZONTAL SCAN LINE (sweep left → right through center)
        // =====================================================
        if (scanActive) {
            // Glow strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = -size.width + (size.width * 2f * scanProgress)
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                NeonCyan.copy(alpha = 0.15f),
                                NeonCyan.copy(alpha = 0.28f),
                                NeonCyan.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Sharp center line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = -size.width + (size.width * 2f * scanProgress)
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                NeonCyan.copy(alpha = 0.7f),
                                NeonCyan,
                                NeonCyan.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // =====================================================
        // LEFT GATE
        // =====================================================
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterStart)
                .graphicsLayer { translationX = -size.width * gateProgress }
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF02070B),
                            Color(0xFF06131D),
                            Color(0xFF0A1F2E)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                NeonCyan,
                                NeonCyan,
                                NeonCyan,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // =====================================================
        // RIGHT GATE
        // =====================================================
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .graphicsLayer { translationX = size.width * gateProgress }
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF0A1F2E),
                            Color(0xFF06131D),
                            Color(0xFF02070B)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                NeonCyan,
                                NeonCyan,
                                NeonCyan,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

private data class SplashParticle(
    val angle: Float,
    val radius: Float,
    val size: Float,
    val alpha: Float
)