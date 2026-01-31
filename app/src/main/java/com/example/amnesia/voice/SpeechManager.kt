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
import java.io.IOException

class SpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onStatusChange: (String) -> Unit
) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    fun startRecordingForEnrollment() {
        startRecording("enroll.m4a")
        onStatusChange("Recording for Enrollment... (Keep holding)")
    }

    fun stopRecordingForEnrollment() {
        stopRecording()
        onStatusChange("Uploading Enrollment...")
        uploadFile(isEnrollment = true)
    }

    fun startRecordingForQuery() {
        startRecording("query.m4a")
        onStatusChange("Listening...")
    }

    fun stopRecordingForQuery() {
        stopRecording()
        onStatusChange("Processing...")
        uploadFile(isEnrollment = false)
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
        } catch (e: IOException) {
            Log.e("SpeechManager", "Record failed: ${e.message}")
            onStatusChange("Error starting mic")
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Handle stop errors (e.g. called too soon)
        }
        mediaRecorder = null
        isRecording = false
    }

    private fun uploadFile(isEnrollment: Boolean) {
        val file = audioFile ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create Multipart Body
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)

                if (isEnrollment) {
                    val response = ApiClient.service.enrollUser(body)
                    if (response.isSuccessful) {
                        onStatusChange("Enrollment Success! You can now speak.")
                    } else {
                        onStatusChange("Enrollment Failed: ${response.code()}")
                    }
                } else {
                    val response = ApiClient.service.processAudio(body)
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.authorized == true) {
                            val text = result.text ?: ""
                            if (text.isNotEmpty()) {
                                onSpeechResult(text)
                                onStatusChange("Success")
                            } else {
                                onStatusChange("Authorized, but heard silence.")
                            }
                        } else {
                            onStatusChange("Ignored: Voice did not match.")
                        }
                    } else {
                        onStatusChange("Server Error: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SpeechManager", "Network Error", e)
                onStatusChange("Network Error: Is server running?")
            }
        }
    }
}