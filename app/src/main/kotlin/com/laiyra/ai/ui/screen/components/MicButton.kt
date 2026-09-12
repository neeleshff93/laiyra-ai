package com.laiyra.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MicButton(
    modifier: Modifier = Modifier
) {

    var isListening by remember {
        mutableStateOf(false)
    }

    val transition = rememberInfiniteTransition(label = "")

    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(55.dp)
                .scale(scale)
                .clickable {
                    isListening = !isListening
                }
                .background(
                    if (isListening) Color.Green else Color.Cyan,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = if (isListening) "ON" else "MIC",
                color = Color.Black,
                fontSize = 12.sp
            )
        }
    }
}