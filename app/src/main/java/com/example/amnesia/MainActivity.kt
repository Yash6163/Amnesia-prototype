package com.example.amnesia

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amnesia.logic.LoopDetector
import com.example.amnesia.logic.LoopResult
import com.example.amnesia.ui.theme.AMNESIATheme
import com.example.amnesia.ui.theme.VoiceButton
import com.example.amnesia.ui.theme.VoiceScreen
import com.example.amnesia.voice.EnrollmentManager
import com.example.amnesia.voice.SmartListener
import com.example.amnesia.voice.TTSManager

enum class AppStep {
    ENROLL_PATIENT,
    ASK_CAREGIVER,
    ENROLL_CAREGIVER,
    LISTENING_MODE
}

class MainActivity : ComponentActivity() {

    private val loopDetector = LoopDetector()
    private lateinit var smartListener: SmartListener
    private lateinit var enrollmentManager: EnrollmentManager
    private lateinit var ttsManager: TTSManager

    // UI State
    private var currentStep by mutableStateOf(AppStep.ENROLL_PATIENT)
    private var statusText by mutableStateOf("Welcome")
    private var loopResult by mutableStateOf<LoopResult?>(null)
    private val historyList = mutableStateListOf<String>()

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) initSpeech() else statusText = "Mic Permission Denied!"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚨 KEEP SCREEN ON FIX (Prevents Background Mic Kill) 🚨
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestPermission.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            AMNESIATheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (currentStep) {

                        // --- 1. ENROLL PATIENT ---
                        AppStep.ENROLL_PATIENT -> {
                            Text("Step 1: Setup", color = Color.Gray)
                            Spacer(Modifier.height(20.dp))
                            Text("Enroll User", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text("Hold button and say:\n'My voice is my password'", textAlign = TextAlign.Center)
                            Spacer(Modifier.height(40.dp))

                            VoiceButton(
                                text = "Hold to Enroll User",
                                color = Color.Blue,
                                onPressDown = {
                                    smartListener.stop()
                                    enrollmentManager.startEnrollment()
                                    statusText = "🎙️ Recording..."
                                },
                                onPressUp = {
                                    statusText = "Uploading..."
                                    enrollmentManager.stopAndUpload("patient") { result ->
                                        runOnUiThread {
                                            statusText = result
                                            if (result.contains("Success")) {
                                                currentStep = AppStep.ASK_CAREGIVER
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(statusText, color = Color.Red)
                        }

                        // --- 2. ASK CAREGIVER ---
                        AppStep.ASK_CAREGIVER -> {
                            Text("Step 2: Caregiver", color = Color.Gray)
                            Spacer(Modifier.height(20.dp))
                            Text("Add a Caregiver?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text("Do you want to enroll a caregiver to help answer questions?", textAlign = TextAlign.Center)
                            Spacer(Modifier.height(40.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(
                                    onClick = { currentStep = AppStep.ENROLL_CAREGIVER },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text("YES, Enroll Now")
                                }

                                Button(
                                    onClick = {
                                        currentStep = AppStep.LISTENING_MODE
                                        startListeningMode()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                ) {
                                    Text("NO, Skip")
                                }
                            }
                        }

                        // --- 3. ENROLL CAREGIVER ---
                        AppStep.ENROLL_CAREGIVER -> {
                            Text("Step 3: Caregiver", color = Color.Gray)
                            Spacer(Modifier.height(20.dp))
                            Text("Enroll Caregiver", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text("Hand phone to caregiver.\nHold button and say:\n'I am the caregiver'", textAlign = TextAlign.Center)
                            Spacer(Modifier.height(40.dp))

                            VoiceButton(
                                text = "Hold to Enroll Caregiver",
                                color = Color(0xFF4CAF50),
                                onPressDown = {
                                    smartListener.stop()
                                    enrollmentManager.startEnrollment()
                                    statusText = "🎙️ Recording Caregiver..."
                                },
                                onPressUp = {
                                    statusText = "Uploading..."
                                    enrollmentManager.stopAndUpload("caregiver") { result ->
                                        runOnUiThread {
                                            statusText = result
                                            if (result.contains("Success")) {
                                                currentStep = AppStep.LISTENING_MODE
                                                startListeningMode()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(statusText, color = Color.Red)
                        }

                        // --- 4. LISTENING MODE ---
                        AppStep.LISTENING_MODE -> {
                            // Only History list here, no buttons
                            VoiceScreen(
                                loopResult = loopResult,
                                history = historyList,
                                statusText = statusText,
                                onEnrollStart = {},
                                onEnrollStop = {},
                                onQueryStart = {},
                                onQueryStop = {}
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initSpeech() {
        ttsManager = TTSManager(this)
        enrollmentManager = EnrollmentManager(this)

        smartListener = SmartListener(
            context = this,
            onSpeechResult = { text, role, isRepeat ->
                runOnUiThread { processInput(text, role, isRepeat) }
            },
            onStatusChange = { status ->
                runOnUiThread { statusText = status }
            }
        )
    }

    private fun startListeningMode() {
        statusText = "👂 Listening Mode Active"
        smartListener.startListening()
    }

    private fun processInput(text: String, role: String?, isRepeat: Boolean?) {
        if (role == "caregiver") {
            statusText = "Saved Answer: $text"
            historyList.add(0, "Caregiver: Answer Saved")
            ttsManager.speak("Answer saved.")
        } else {
            val result = loopDetector.processInput(text)
            loopResult = result
            historyList.add(0, "Patient: $text")

            if (isRepeat == true) {
                ttsManager.speak("You just asked that. Playing answer...")
                statusText = "🔁 Repetition: Playing Answer"
            } else if (result is LoopResult.Repeated) {
                ttsManager.speak("You just asked that.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        smartListener.stop()
        ttsManager.shutdown()
    }
}