package com.example.amnesia

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.amnesia.logic.LoopDetector
import com.example.amnesia.logic.LoopResult
import com.example.amnesia.ui.theme.AMNESIATheme
import com.example.amnesia.ui.theme.VoiceScreen
import com.example.amnesia.voice.SpeechManager
import com.example.amnesia.voice.TTSManager

class MainActivity : ComponentActivity() {

    private val loopDetector = LoopDetector()

    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TTSManager

    private var isSpeechReady by mutableStateOf(false)

    private var loopResult by mutableStateOf<LoopResult?>(null)
    private val historyList = mutableStateListOf<String>()
    private var statusText by mutableStateOf("Idle")

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                initSpeech()
            } else {
                statusText = "Mic Permission Denied!"
            }
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
                        if (isSpeechReady) {
                            Log.d("UI", "Enroll start")
                            speechManager.startRecordingForEnrollment()
                        }
                    },

                    onEnrollStop = {
                        if (isSpeechReady) {
                            speechManager.stopRecording()
                        }
                    },

                    onQueryStart = {
                        if (isSpeechReady) {
                            Log.d("UI", "Query start")
                            speechManager.startRecordingForQuery()
                        }
                    },

                    onQueryStop = {
                        if (isSpeechReady) {
                            speechManager.stopRecording()
                        }
                    }
                )
            }
        }
    }

    private fun initSpeech() {
        Log.d("MainActivity", "initSpeech() called")

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

        isSpeechReady = true
        Log.d("MainActivity", "Speech READY")
    }

    private fun processInput(text: String) {
        val result = loopDetector.processInput(text)
        loopResult = result

        historyList.add(0, text)
        if (historyList.size > 10) {
            historyList.removeAt(historyList.lastIndex)
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
