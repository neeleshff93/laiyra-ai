package com.laiyra.ai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HomeChatBox(
    speechText: String,
    isListening: Boolean,
    onMicClick: () -> Unit,
    messages: List<Pair<Boolean, String>>,
    liveAiReply: String = ""
) {
    val listState = rememberLazyListState()

    val lastAiInHistory = messages.lastOrNull { !it.first }?.second
    val showLiveAi = liveAiReply.isNotEmpty() && liveAiReply != lastAiInHistory

    val totalItems = messages.size +
            (if (speechText.isNotEmpty()) 1 else 0) +
            (if (showLiveAi) 1 else 0)

    LaunchedEffect(totalItems) {
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .shadow(14.dp, RoundedCornerShape(22.dp))
            .background(PanelColor, RoundedCornerShape(22.dp))
            .border(2.dp, NeonCyan, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            val isEmpty = messages.isEmpty() && speechText.isEmpty() && !showLiveAi

            if (isEmpty) {
                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("♙  YOU", color = NeonCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Tap the mic and speak...", color = SoftText, fontSize = 16.sp)
                    }

                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF123546)))

                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⌁  LAIYRA", color = NeonCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Waiting...", color = SoftText, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(messages.size) { index ->
                        val msg = messages[index]
                        ChatMessage(isUser = msg.first, text = msg.second)
                    }

                    if (speechText.isNotEmpty()) {
                        item { ChatMessage(isUser = true, text = speechText, live = true) }
                    }

                    if (showLiveAi) {
                        item { ChatMessage(isUser = false, text = liveAiReply, live = true) }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clickable { onMicClick() }
                        .shadow(15.dp, CircleShape)
                        .background(if (isListening) Color.Red else NeonCyan, CircleShape)
                        .border(3.dp, Color(0xFF00A9BC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isListening) "●" else "♩",
                        color = Color.Black,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessage(
    isUser: Boolean,
    text: String,
    live: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(
            text = if (isUser) "♙  " else "⌁  ",
            color = NeonCyan,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text + if (live) "▌" else "",
            color = if (isUser) Color.White else NeonCyan,
            fontSize = 15.sp
        )
    }
}