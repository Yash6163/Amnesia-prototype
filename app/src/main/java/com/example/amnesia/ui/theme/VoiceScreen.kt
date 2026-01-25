package com.example.amnesia.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amnesia.logic.LoopResult

@Composable
fun VoiceScreen(
    loopResult: LoopResult?,
    history: List<String>, // Receives history list
    onMicClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. STATUS CARD ---
        if (loopResult != null) {
            StatusCard(loopResult)
        } else {
            // Idle State
            Text(
                "Tap microphone to start monitoring",
                color = Color.Gray,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. HISTORY LIST (New Feature) ---
        if (history.isNotEmpty()) {
            Text(
                text = "Recent Interaction Trace:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Fill available space
                    .fillMaxWidth()
            ) {
                items(history) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. MIC BUTTON ---
        Button(
            onClick = onMicClick,
            modifier = Modifier
                .size(width = 220.dp, height = 70.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("🎤 Monitor Input", fontSize = 20.sp)
        }
    }
}

@Composable
fun StatusCard(result: LoopResult) {
    val (bgColor, icon, title, mainText, subText) = when (result) {
        is LoopResult.Recorded -> {
            FiveTuple(
                Color(0xFFE8F5E9), "✅", "Input Recorded",
                "\"${result.text}\"", "Waiting for next input..."
            )
        }
        is LoopResult.Repeated -> {
            FiveTuple(
                Color(0xFFFFEBEE), "⚠️", "Repetition Detected",
                "\"${result.currentText}\"",
                "Similar to: \"${result.previousText}\"\n(Asked ${result.secondsAgo}s ago)"
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = mainText, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Color.Black)

            if (subText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = subText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color.DarkGray)
            }
        }
    }
}

data class FiveTuple(val c: Color, val i: String, val t: String, val m: String, val s: String)