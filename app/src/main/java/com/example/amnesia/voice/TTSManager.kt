package com.example.amnesia.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale


class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isReady = true
        }
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "AMNESIA_TTS"
        )
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
