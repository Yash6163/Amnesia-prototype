package com.example.amnesia.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onResult: (String) -> Unit
) {

    private val recognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SPEECH", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SPEECH", "Speech started")
            }

            override fun onResults(results: Bundle?) {
                isListening = false

                val text =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()

                Log.d("SPEECH", "Recognized: $text")

                if (!text.isNullOrBlank()) {
                    // ✅ ALWAYS switch to UI thread
                    mainHandler.post {
                        onResult(text)
                    }
                }
            }

            override fun onError(error: Int) {
                isListening = false
                Log.e("SPEECH", "Error code: $error")
            }

            override fun onEndOfSpeech() {
                Log.d("SPEECH", "End of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SPEECH", "Speech recognition not available")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        // ❌ DO NOT call cancel()
        recognizer.startListening(intent)
        isListening = true
    }

    fun stopListening() {
        if (isListening) {
            recognizer.stopListening()
            isListening = false
        }
    }

    fun destroy() {
        recognizer.stopListening()
        recognizer.destroy()
    }
}
