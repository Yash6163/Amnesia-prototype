package com.example.amnesia

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.amnesia.logic.LoopDetector
import com.example.amnesia.logic.LoopResult
import com.example.amnesia.ui.theme.AMNESIATheme
import com.example.amnesia.ui.theme.VoiceScreen
import com.example.amnesia.voice.SpeechManager
import com.example.amnesia.voice.TTSManager

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TTSManager
    private val loopDetector = LoopDetector()

    // UI State
    private var loopResult by mutableStateOf<LoopResult?>(null)
    private val historyList = mutableStateListOf<String>()
    private var statusText by mutableStateOf("Idle")

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) statusText = "Mic Permission Denied!"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermission.launch(Manifest.permission.RECORD_AUDIO)

        ttsManager = TTSManager(this)

        speechManager = SpeechManager(
            context = this,
            onSpeechResult = { text ->
                runOnUiThread { processInput(text) }
            },
            onStatusChange = { status ->
                runOnUiThread { statusText = status }
            }
        )

        setContent {
            AMNESIATheme {
                VoiceScreen(
                    loopResult = loopResult,
                    history = historyList,
                    statusText = statusText,
                    onEnrollStart = { speechManager.startRecordingForEnrollment() },
                    onEnrollStop = { speechManager.stopRecordingForEnrollment() },
                    onQueryStart = { speechManager.startRecordingForQuery() },
                    onQueryStop = { speechManager.stopRecordingForQuery() }
                )
            }
        }
    }

    private fun processInput(text: String) {
        val result = loopDetector.processInput(text)
        loopResult = result

        historyList.add(0, text)
        if (historyList.size > 10) {
            historyList.removeAt(historyList.lastIndex) // minSdk-safe
        }

        if (result is LoopResult.Repeated) {
            ttsManager.speak("You just asked that.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
