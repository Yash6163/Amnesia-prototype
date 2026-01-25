package com.example.amnesia

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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

    private var speechManager: SpeechManager? = null
    private lateinit var ttsManager: TTSManager
    private val loopDetector = LoopDetector()

    // UI State
    private var loopResult by mutableStateOf<LoopResult?>(null)
    private val historyList = mutableStateListOf<String>()

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startFreshListeningSession()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager = TTSManager(this)

        setContent {
            AMNESIATheme {
                VoiceScreen(
                    loopResult = loopResult,
                    history = historyList,
                    onMicClick = { checkMicPermissionAndStart() }
                )
            }
        }
    }

    private fun checkMicPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                startFreshListeningSession()
            }
            else -> {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startFreshListeningSession() {
        speechManager?.destroy()
        speechManager = SpeechManager(this) { text ->
            runOnUiThread { processInput(text) }
        }
        speechManager?.startListening()
    }

    private fun processInput(text: String) {
        val result = loopDetector.processInput(text)
        loopResult = result

        // Update history
        historyList.add(0, text)
        if (historyList.size > 10) historyList.removeLast()

        when (result) {
            is LoopResult.Recorded -> {
                ttsManager.speak("Okay.")
            }
            is LoopResult.Repeated -> {
                ttsManager.speak("Error. Repetition detected. You just asked that.")
                triggerErrorVibration()
            }
        }
    }

    private fun triggerErrorVibration() {
        // SAFETY FIX: Try-Catch block prevents crash if permission is missing
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 100, 100, 300)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            Log.e("MAIN", "Vibration failed (App did not crash): ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager?.destroy()
        ttsManager.shutdown()
    }
}