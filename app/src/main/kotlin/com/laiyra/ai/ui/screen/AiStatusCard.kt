package com.laiyra.ai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AiStatusCard(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(190.dp)
            .padding(start = 5.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp)
            )
            .background(PanelColor, RoundedCornerShape(22.dp))
            .border(2.dp, NeonCyan, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AI",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 9.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Purple, NeonCyan)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ON",
                color = NeonCyan,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

