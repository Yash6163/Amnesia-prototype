package com.example.amnesia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoiceScreen(
    spokenText: String,
    answer: String?,
    onMicClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "You said:")
        Text(text = spokenText.ifEmpty { "—" })

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Answer:")
        Text(text = answer ?: "—")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onMicClick) {
            Text("🎤 Tap to Speak")
        }
    }
}