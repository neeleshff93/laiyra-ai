package com.laiyra.ai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BottomNavigation() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp)
            )
            .background(Color(0xFF061522), RoundedCornerShape(28.dp))
            .border(1.dp, Color(0xFF006B91), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem(icon = "⌂", text = "Home", active = true)
            BottomItem(icon = "▤", text = "Chat", active = false)
            BottomItem(icon = "▦", text = "Tools", active = false)
            BottomItem(icon = "♙", text = "Profile", active = false)
        }
    }
}

@Composable
internal fun BottomItem(
    icon: String,
    text: String,
    active: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = icon,
            color = if (active) NeonCyan else Color(0xFFB7C7DE),
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = if (active) NeonCyan else Color(0xFFB7C7DE),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        if (active) {
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 3.dp)
                    .background(NeonCyan, RoundedCornerShape(3.dp))
            )
        }
    }
}

