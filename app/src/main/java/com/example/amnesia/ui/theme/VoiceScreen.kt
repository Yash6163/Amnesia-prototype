package com.example.amnesia.ui.theme

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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- STATUS CARD ---
        if (loopResult != null) {
            StatusCard(loopResult)
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Ready to Listen", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Status: $statusText",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Blue
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- BUTTONS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            // ENROLL BUTTON
            Button(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onEnrollStart()
                                tryAwaitRelease()
                                onEnrollStop()
                            }
                        )
                    },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text("Hold to Enroll\n(Voice ID)", textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // QUERY BUTTON
            Button(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onQueryStart()
                                tryAwaitRelease()
                                onQueryStop()
                            }
                        )
                    },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Hold to Speak\n(Query)", textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- HISTORY ---
        Text(
            text = "Recent History:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(history) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(result: LoopResult) {

    val (bgColor, icon, title, mainText) = when (result) {
        is LoopResult.Recorded -> {
            FourTuple(
                Color(0xFFE8F5E9),
                "✅",
                "Input Recorded",
                result.text
            )
        }

        is LoopResult.Repeated -> {
            FourTuple(
                Color(0xFFFFEBEE),
                "⚠️",
                "Repetition Detected",
                result.currentText
            )
        }

        else -> {
            FourTuple(
                Color.LightGray,
                "ℹ️",
                "Idle",
                ""
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
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mainText,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

data class FourTuple(
    val bg: Color,
    val icon: String,
    val title: String,
    val message: String
)
