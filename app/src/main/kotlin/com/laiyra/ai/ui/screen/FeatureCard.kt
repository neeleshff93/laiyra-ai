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
internal fun FeatureRow(
    leftTitle: String,
    leftSubtitle: String,
    leftIcon: String,
    rightTitle: String,
    rightSubtitle: String,
    rightIcon: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureCard(
            modifier = Modifier.weight(1f),
            title = leftTitle,
            subtitle = leftSubtitle,
            icon = leftIcon
        )
        FeatureCard(
            modifier = Modifier.weight(1f),
            title = rightTitle,
            subtitle = rightSubtitle,
            icon = rightIcon
        )
    }
}

@Composable
internal fun FeatureCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: String
) {
    Box(
        modifier = modifier
            .height(126.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .background(PanelColor, RoundedCornerShape(20.dp))
            .border(2.dp, NeonCyan, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                color = NeonCyan,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFA8BCD2),
                    fontSize = 12.sp
                )
            }

            Text(
                text = "›",
                color = Color.White,
                fontSize = 30.sp
            )
        }
    }
}

