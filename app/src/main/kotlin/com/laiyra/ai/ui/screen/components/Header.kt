package com.laiyra.ai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
internal fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
            )
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF071727),
                        Color(0xFF03101B)
                    )
                ),
                shape = RoundedCornerShape(
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFF007C9B),
                shape = RoundedCornerShape(
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
            )
            .padding(
                start = 16.dp,
                end = 14.dp,
                bottom = 16.dp
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "LAIYRA",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " AI",
                        color = NeonCyan,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "⚙",
                    color = NeonCyan,
                    fontSize = 34.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Y O U R   P E R S O N A L   A I   A S S I S T A N T",
                color = Color(0xFF9FB6D1),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
