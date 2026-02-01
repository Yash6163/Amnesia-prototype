package com.example.amnesia.voice

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.example.amnesia.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onStatusChange: (String) -> Unit
) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var isEnrollmentMode = false

    fun startRecordingForEnrollment() {
        isEnrollmentMode = true
        startRecording("enroll.m4a")
        onStatusChange("Recording Voice ID...")
    }

    fun startRecordingForQuery() {
        isEnrollmentMode = false
        startRecording("query.m4a")
        onStatusChange("Listening...")
    }

    fun stopRecording() {
        stopRecordingInternal()
    }

    private fun startRecording(fileName: String) {
        try {
            audioFile = File(context.cacheDir, fileName)
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Log.d("SpeechManager", "✅ Mic Started: ${audioFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("SpeechManager", "❌ Mic Failed", e)
            onStatusChange("Mic Error")
        }
    }

    private fun stopRecordingInternal() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {}
        mediaRecorder = null
        isRecording = false

        onStatusChange("Uploading...")
        uploadFile(isEnrollmentMode)
    }

    private fun uploadFile(isEnrollment: Boolean) {
        val file = audioFile ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SpeechManager", "📤 Uploading to ${ApiClient.BASE_URL}")
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)

                val response = if (isEnrollment) ApiClient.service.enrollUser(body)
                else ApiClient.service.processAudio(body)

                if (response.isSuccessful) {
                    if (isEnrollment) onStatusChange("Enrollment Success!")
                    else {
                        val result = response.body() as? com.example.amnesia.network.ProcessResponse
                        onSpeechResult(result?.text ?: "")
                        onStatusChange("Success")
                    }
                } else {
                    onStatusChange("Server Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SpeechManager", "❌ Network Error: ${e.message}")
                onStatusChange("Failed to Connect")
            }
        }
    }
}