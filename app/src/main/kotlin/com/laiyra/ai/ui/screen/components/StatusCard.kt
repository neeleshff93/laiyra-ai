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
fun StatusCard(
    title: String,
    value: String
) {

    Card(
        modifier = Modifier
            .size(130.dp)
            .border(
                2.dp,
                Color.Cyan,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF071420)
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = title,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LinearProgressIndicator(
           modifier = Modifier.fillMaxWidth(),
           progress = 0.8f
      )
 
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = value,
                color = Color.Cyan
            )
        }
    }
}