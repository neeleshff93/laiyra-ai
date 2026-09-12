package com.laiyra.ai.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    viewModel: LaiyraViewModel = viewModel()
) {
    val isListening by viewModel.isListening.collectAsState()
    val speechText by viewModel.speechText.collectAsState()
    val liveAiReply by viewModel.liveAiReply.collectAsState()
    val messages = ConversationMemory.messages

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        ) {
            Header()
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CpuCard(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.weight(1.15f),
                    contentAlignment = Alignment.Center
                ) { AiCore() }
                AiStatusCard(modifier = Modifier.weight(0.45f))
            }

            Spacer(Modifier.height(12.dp))

            HomeChatBox(
                speechText = speechText,
                isListening = isListening,
                onMicClick = { viewModel.toggleMic() },
                messages = messages,
                liveAiReply = liveAiReply
            )

            Spacer(Modifier.height(14.dp))

            FeatureRow("CHAT", "Talk to Laiyra", "◌", "AI SEARCH", "Search anything", "⌕")
            Spacer(Modifier.height(12.dp))
            FeatureRow("IMAGE AI", "Generate images", "▧", "WEB", "Browse the web", "◎")
            Spacer(Modifier.height(18.dp))
            BottomNavigation()
        }
    }
}