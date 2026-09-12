package com.laiyra.ai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FeatureCard(
    title: String
) {

    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .height(100.dp)
            .border(
                2.dp,
                Color.Cyan,
                RoundedCornerShape(16.dp)
            ),

        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF081722)
        )
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = title,
                color = Color.White
            )
        }
    }
}