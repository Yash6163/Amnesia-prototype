package com.example.amnesia.ui.theme

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amnesia.logic.LoopResult

@Composable
fun VoiceScreen(
    loopResult: LoopResult?,
    history: List<String>,
    statusText: String,
    onEnrollStart: () -> Unit,
    onEnrollStop: () -> Unit,
    onQueryStart: () -> Unit,
    onQueryStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (loopResult != null) {
            StatusCard(loopResult)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ready to Listen", color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Status: $statusText", color = Color.Blue)
        Spacer(Modifier.height(16.dp))

        // ... inside VoiceScreen ...

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

            // ✅ FIXED: Changed color from 'primary' to 'Color.Blue' so it is visible
            VoiceButton(
                text = "Hold to Enroll\n(Voice ID)",
                color = Color.Blue,
                onPressDown = onEnrollStart,
                onPressUp = onEnrollStop,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp))

            VoiceButton(
                text = "Hold to Speak\n(Query)",
                color = Color.DarkGray, // Changed to DarkGray for contrast
                onPressDown = onQueryStart,
                onPressUp = onQueryStop,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent History:", modifier = Modifier.align(Alignment.Start))

        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(history) { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(item, Modifier.padding(12.dp))
                }
            }
        }
    }
}

// ✅ NEW COMPONENT: A Card that acts like a Button but allows "Hold" gestures
@Composable
fun VoiceButton(
    text: String,
    color: Color,
    onPressDown: () -> Unit,
    onPressUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(50), // Makes it pill-shaped like a button
        modifier = modifier
            .height(60.dp) // Fixed height for touch target
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            Log.d("UI", "Button Pressed: $text")
                            onPressDown() // Trigger Start
                            awaitRelease() // Wait for user to lift finger
                        } finally {
                            Log.d("UI", "Button Released: $text")
                            onPressUp() // Trigger Stop
                        }
                    }
                )
            }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun StatusCard(result: LoopResult) {
    val (bg, icon, title, text) = when (result) {
        is LoopResult.Recorded ->
            FourTuple(Color(0xFFE8F5E9), "✅", "Input Recorded", result.text)

        is LoopResult.Repeated ->
            FourTuple(Color(0xFFFFEBEE), "⚠️", "Repetition Detected", result.currentText)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp)
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(text, textAlign = TextAlign.Center)
        }
    }
}

data class FourTuple(
    val bg: Color,
    val icon: String,
    val title: String,
    val message: String
)