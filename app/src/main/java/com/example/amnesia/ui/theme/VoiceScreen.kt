// 🔴 PAY ATTENTION TO THIS LINE:
package com.example.amnesia.ui.theme

import androidx.compose.foundation.layout.*
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
// Ensure this import matches where you actually created LoopResult.kt
// If LoopResult is red, delete this import and press Alt+Enter on LoopResult in the code
import com.example.amnesia.logic.LoopResult

@Composable
fun VoiceScreen(
    loopResult: LoopResult?,
    onMicClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loopResult != null) {
            StatusCard(loopResult)
        } else {
            Text("Tap microphone to start monitoring", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = onMicClick,
            modifier = Modifier.size(width = 220.dp, height = 70.dp),
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
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f)) // Note: 'Divider' was renamed to 'HorizontalDivider' in newer Compose versions
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = subText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color.DarkGray)
            }
        }
    }
}

data class FiveTuple(val c: Color, val i: String, val t: String, val m: String, val s: String)