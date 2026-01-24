package com.example.amnesia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.amnesia.data.ConvoStore
import com.example.amnesia.ui.VoiceScreen
import com.example.amnesia.ui.theme.AMNESIATheme
import com.example.amnesia.voice.SpeechManager
import com.example.amnesia.voice.TTSManager

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TTSManager
    private val convoStore = ConvoStore()

    private var spokenText by mutableStateOf("")
    private var answer by mutableStateOf<String?>(null)

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                speechManager.startListening()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dummy memory (example)
        convoStore.addConvo(
            "what do you mean by ai",
            "Artificial Intelligence is the ability of machines to mimic human intelligence.",
            "10:00"
        )

        ttsManager = TTSManager(this)

        speechManager = SpeechManager(this) { text ->
            runOnUiThread {
                // 🔥 VERY IMPORTANT
                speechManager.stopListening()

                spokenText = text
                answer = convoStore.returnResultIfFound(text) ?: "I don't know yet"
                ttsManager.speak(answer!!)
            }
        }

        setContent {
            AMNESIATheme {
                VoiceScreen(
                    spokenText = spokenText,
                    answer = answer,
                    onMicClick = { checkMicPermissionAndStart() }
                )
            }
        }
    }

    private fun checkMicPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
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
