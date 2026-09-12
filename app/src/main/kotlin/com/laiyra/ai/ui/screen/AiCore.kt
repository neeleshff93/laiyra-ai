package com.laiyra.ai.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal data class CoreParticle(
    val angle: Float,
    val radius: Float,
    val size: Float
)

@Composable
internal fun AiCore() {
    val transition = rememberInfiniteTransition(label = "core")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val particles = remember {
        List(1000) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val radius = Random.nextFloat() * 73f
            CoreParticle(
                angle = angle,
                radius = radius,
                size = Random.nextFloat() * 1.35f + 0.45f
            )
        }
    }

    Box(
        modifier = Modifier.size(205.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = NeonCyan,
                radius = size.minDimension / 2f - 5f,
                style = Stroke(width = 3.5f)
            )

            drawCircle(
                color = NeonCyan.copy(alpha = 0.65f),
                radius = size.minDimension / 2f - 15f,
                style = Stroke(width = 2.5f)
            )

            drawCircle(
                color = NeonCyan.copy(alpha = 0.42f),
                radius = size.minDimension / 2f - 29f,
                style = Stroke(width = 1.8f)
            )

            drawCircle(
                color = NeonCyan.copy(alpha = 0.28f),
                radius = size.minDimension / 2f - 48f,
                style = Stroke(width = 1.4f)
            )

            val markRadius = size.minDimension / 2f - 7f

            repeat(8) { index ->
                val angle = index * (Math.PI * 2.0 / 8.0)
                val x = center.x + cos(angle).toFloat() * markRadius
                val y = center.y + sin(angle).toFloat() * markRadius

                drawCircle(
                    color = NeonCyan,
                    radius = 3.2f,
                    center = Offset(x.toFloat(), y.toFloat())
                )
            }

            particles.forEach { particle ->
                val x = center.x + cos(particle.angle).toFloat() * particle.radius
                val y = center.y + sin(particle.angle).toFloat() * particle.radius

                drawCircle(
                    color = NeonCyan.copy(alpha = 0.58f),
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }

        Text(
            text = "LA",
            color = NeonCyan,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

