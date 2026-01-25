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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.amnesia.data.LoopEvent
import com.example.amnesia.data.MemoryDatabase
import com.example.amnesia.logic.LoopDetector
import com.example.amnesia.logic.LoopResult
import com.example.amnesia.ui.theme.AnalyticsScreen
import com.example.amnesia.ui.theme.VoiceScreen
import com.example.amnesia.ui.theme.AMNESIATheme
import com.example.amnesia.voice.SpeechManager
import com.example.amnesia.voice.TTSManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var speechManager: SpeechManager? = null
    private lateinit var ttsManager: TTSManager
    private val loopDetector = LoopDetector()

    // Database & UI State
    private lateinit var database: MemoryDatabase
    private var showAnalytics by mutableStateOf(false)
    private val analyticsEvents = mutableStateListOf<LoopEvent>() // Live list from DB

    // Core UI State
    private var loopResult by mutableStateOf<LoopResult?>(null)
    private val historyList = mutableStateListOf<String>()

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startFreshListeningSession()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize DB
        database = MemoryDatabase.getDatabase(this)
        loadAnalyticsData() // Load saved data immediately

        ttsManager = TTSManager(this)

        setContent {
            AMNESIATheme {
                if (showAnalytics) {
                    // --- SCREEN 2: ANALYTICS ---
                    AnalyticsScreen(
                        events = analyticsEvents,
                        onBackClick = { showAnalytics = false },
                        onClearClick = { clearAnalyticsData() }
                    )
                } else {
                    // --- SCREEN 1: MAIN VOICE INTERFACE ---
                    Box(modifier = Modifier.fillMaxSize()) {
                        VoiceScreen(
                            loopResult = loopResult,
                            history = historyList,
                            onMicClick = { checkMicPermissionAndStart() }
                        )

                        // 📊 Small "Doctor Button" in Top Right
                        Button(
                            onClick = { showAnalytics = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                        ) {
                            Text("📊")
                        }
                    }
                }
            }
        }
    }

    private fun loadAnalyticsData() {
        lifecycleScope.launch {
            database.loopDao().getAllEvents().collect { list ->
                analyticsEvents.clear()
                analyticsEvents.addAll(list)
            }
        }
    }

    private fun clearAnalyticsData() {
        lifecycleScope.launch {
            database.loopDao().clearAll()
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

        // UI History Update
        historyList.add(0, text)

        // --- FIX FOR API 35 ERROR ---
        // We use removeAt(index) instead of removeLast() because it works on all Android versions
        if (historyList.size > 10) {
            historyList.removeAt(historyList.lastIndex)
        }

        when (result) {
            is LoopResult.Recorded -> {
                ttsManager.speak("Okay.")
            }
            is LoopResult.Repeated -> {
                ttsManager.speak("Error. Repetition detected.")
                triggerErrorVibration()

                // --- SAVE ERROR TO DB ---
                val event = LoopEvent(
                    timestamp = System.currentTimeMillis(),
                    text = result.currentText,
                    previousText = result.previousText
                )
                lifecycleScope.launch {
                    database.loopDao().insert(event)
                }
            }
        }
    }

    private fun triggerErrorVibration() {
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
            Log.e("MAIN", "Vib failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager?.destroy()
        ttsManager.shutdown()
    }
}