package com.example.amnesia.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.amnesia.network.ApiClient
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class SmartListener(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onStatusChange: (String) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.IO)

    // Audio Config
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val CHUNK_SIZE = 1024

    // Logic State
    private val pcmBuffer = mutableListOf<Short>()
    private var isSpeaking = false
    private var silenceCounter = 0

    // 🎚️ SENSITIVITY SETTINGS
    // If volume goes above this, we start recording.
    private val VOICE_THRESHOLD = 500
    // How many "silent" chunks before we stop (approx 1.5 seconds)
    private val SILENCE_PATIENCE = 30

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isListening) return
        isListening = true
        onStatusChange("👂 Always-On Listening...")

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        // Ensure buffer is large enough
        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE * 2)

        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
        audioRecord?.startRecording()

        scope.launch {
            val buffer = ShortArray(CHUNK_SIZE)

            while (isListening) {
                val read = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: 0
                if (read > 0) {
                    processAudioChunk(buffer, read)
                }
            }
        }
    }

    private fun processAudioChunk(buffer: ShortArray, readSize: Int) {
        // 1. Calculate Loudness (RMS)
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += buffer[i] * buffer[i]
        }
        val amplitude = sqrt(sum / readSize)

        // 2. Decide if it's Speech
        val isLoud = amplitude > VOICE_THRESHOLD

        if (isLoud) {
            if (!isSpeaking) {
                isSpeaking = true
                onStatusChange("🗣️ Speech Detected...")
                pcmBuffer.clear()
            }
            silenceCounter = 0
            // Add this chunk
            for (i in 0 until readSize) pcmBuffer.add(buffer[i])
        } else {
            if (isSpeaking) {
                // We are in a quiet pause, keep recording briefly
                for (i in 0 until readSize) pcmBuffer.add(buffer[i])
                silenceCounter++

                if (silenceCounter > SILENCE_PATIENCE) {
                    // Silence has lasted long enough. STOP and UPLOAD.
                    isSpeaking = false
                    onStatusChange("Processing...")
                    saveAndUpload()
                }
            }
        }
    }

    private fun saveAndUpload() {
        if (pcmBuffer.size < SAMPLE_RATE / 2) {
            // Ignore very short audio glitches (< 0.5s)
            onStatusChange("👂 Listening...")
            return
        }

        val file = File(context.cacheDir, "query.wav")
        saveWavFile(file, pcmBuffer.toShortArray())

        scope.launch {
            try {
                val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)

                val response = ApiClient.service.processAudio(body)
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.authorized == true) {
                        onSpeechResult(result.text ?: "")
                        onStatusChange("✅ Verified: ${result.text}")
                    } else {
                        onStatusChange("⛔ Ignored (Stranger)")
                    }
                } else {
                    onStatusChange("Server Error")
                }
                // Resume listening status after a moment
                delay(1500)
                onStatusChange("👂 Listening...")
            } catch (e: Exception) {
                Log.e("SmartListener", "Upload Error", e)
                onStatusChange("Connection Failed")
            }
        }
    }

    fun stop() {
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
    }

    // --- UTILITY: Create Valid WAV File Header ---
    private fun saveWavFile(file: File, data: ShortArray) {
        val byteData = ByteBuffer.allocate(data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        data.forEach { byteData.putShort(it) }

        val pcmData = byteData.array()
        val header = wavHeader(pcmData.size.toLong(), SAMPLE_RATE.toLong())

        FileOutputStream(file).use {
            it.write(header)
            it.write(pcmData)
        }
    }

    private fun wavHeader(totalAudioLen: Long, longSampleRate: Long): ByteArray {
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = longSampleRate * 16 * channels / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte(); header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte(); header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte(); header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte(); header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte(); header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte(); header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        return header
    }
}