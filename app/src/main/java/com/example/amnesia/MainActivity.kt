package com.example.amnesia

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.amnesia.logic.LoopDetector
import com.example.amnesia.logic.LoopResult
import com.example.amnesia.ui.theme.AMNESIATheme
import com.example.amnesia.ui.theme.VoiceScreen
import com.example.amnesia.voice.EnrollmentManager
import com.example.amnesia.voice.SmartListener
import com.example.amnesia.voice.TTSManager

class MainActivity : ComponentActivity() {

    private val loopDetector = LoopDetector()
    private lateinit var smartListener: SmartListener
    private lateinit var enrollmentManager: EnrollmentManager
    private lateinit var ttsManager: TTSManager

    // State
    private var loopResult by mutableStateOf<LoopResult?>(null)
    private val historyList = mutableStateListOf<String>()
    private var statusText by mutableStateOf("Ready to Enroll") // Changed initial status

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) initSpeech() else statusText = "Mic Permission Denied!"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            AMNESIATheme {
                VoiceScreen(
                    loopResult = loopResult,
                    history = historyList,
                    statusText = statusText,
                    onEnrollStart = {
                        // Ensure background listener is OFF before recording
                        smartListener.stop()
                        enrollmentManager.startEnrollment()
                        statusText = "🎙️ Recording Voice ID..."
                    },
                    onEnrollStop = {
                        statusText = "Uploading..."
                        enrollmentManager.stopAndUpload { result ->
                            runOnUiThread {
                                statusText = result
                                // ✅ ONLY Start Listening if Enrollment Succeeded
                                if (result.contains("Success")) {
                                    smartListener.startListening()
                                }
                            }
                        }
                    },
                    onQueryStart = {},
                    onQueryStop = {}
                )
            }
        }
    }

    private fun initSpeech() {
        ttsManager = TTSManager(this)
        enrollmentManager = EnrollmentManager(this)

        smartListener = SmartListener(
            context = this,
            onSpeechResult = { text ->
                runOnUiThread { processInput(text) }
            },
            onStatusChange = { status ->
                runOnUiThread { statusText = status }
            }
        )

        // ❌ REMOVED: smartListener.startListening()
        // We now wait for the user to enroll first.
    }

    private fun processInput(text: String) {
        val result = loopDetector.processInput(text)
        loopResult = result
        historyList.add(0, text)
        if (result is LoopResult.Repeated) {
            ttsManager.speak("You just asked that.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        smartListener.stop()
        ttsManager.shutdown()
    }
}