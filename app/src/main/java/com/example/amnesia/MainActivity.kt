package com.example.amnesia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.amnesia.logic.LoopDetector
import com.example.amnesia.logic.LoopResult
import com.example.amnesia.ui.theme.VoiceScreen
import com.example.amnesia.ui.theme.AMNESIATheme
import com.example.amnesia.voice.SpeechManager
import com.example.amnesia.voice.TTSManager

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TTSManager

    // Logic: Pure RAM loop detector
    private val loopDetector = LoopDetector()

    // UI State
    private var loopResult by mutableStateOf<LoopResult?>(null)

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) speechManager.startListening()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Text-to-Speech
        ttsManager = TTSManager(this)

        // Initialize Speech Recognition
        speechManager = SpeechManager(this) { text ->
            runOnUiThread {
                // 1. Process the input using your Loop Logic
                val result = loopDetector.processInput(text)

                // 2. Update the UI (Green or Red Card)
                loopResult = result

                // 3. Audio Feedback Logic
                when (result) {
                    is LoopResult.Recorded -> {
                        // 🟢 Case 1: New Input
                        // Just a short confirmation so the user knows it worked.
                        // (You can remove this line if you want total silence for new inputs)
                        ttsManager.speak("Okay.")
                    }
                    is LoopResult.Repeated -> {
                        // 🔴 Case 2: Repetition Detected
                        // This speaks the ERROR message you requested.
                        ttsManager.speak("Error. Repetition detected. You just asked that.")
                    }
                }
            }
        }

        setContent {
            AMNESIATheme {
                VoiceScreen(
                    loopResult = loopResult,
                    onMicClick = { checkMicPermissionAndStart() }
                )
            }
        }
    }

    private fun checkMicPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                speechManager.startListening()
            }
            else -> {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}