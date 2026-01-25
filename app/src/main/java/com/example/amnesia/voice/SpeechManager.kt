package com.example.amnesia.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager // <--- This is likely the missing import
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening() {
        destroy()

        // Force Audio Hardware Wakeup
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.stopBluetoothSco()
            audioManager.isMicrophoneMute = false
        } catch (e: Exception) {
            Log.e("SPEECH", "Could not reset Audio Manager: ${e.message}")
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }

            Log.d("SPEECH", "Starting fresh listener...")
            speechRecognizer?.startListening(intent)
            isListening = true
        } else {
            Log.e("SPEECH", "Recognition not available on this device")
        }
    }

    fun stopListening() {
        if (isListening) {
            Log.d("SPEECH", "Stopping listener")
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SPEECH", "Error destroying recognizer: ${e.message}")
        }
        speechRecognizer = null
        isListening = false
    }

    override fun onReadyForSpeech(params: Bundle?) { Log.d("SPEECH", "Ready") }
    override fun onBeginningOfSpeech() { Log.d("SPEECH", "Started") }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { isListening = false }

    override fun onError(error: Int) {
        Log.e("SPEECH", "Error code: $error")
        isListening = false
        destroy()
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotEmpty()) {
            onSpeechResult(text)
        }
        destroy()
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}