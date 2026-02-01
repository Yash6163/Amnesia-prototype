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

class EnrollmentManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startEnrollment() {
        audioFile = File(context.cacheDir, "enroll.m4a")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
                Log.d("Enrollment", "Recording started")
            } catch (e: IOException) {
                Log.e("Enrollment", "Failed to start", e)
            }
        }
    }

    fun stopAndUpload(onResult: (String) -> Unit) {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Handle crash if stopped too soon
        }
        mediaRecorder = null

        // Upload to /enroll endpoint
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = audioFile ?: return@launch
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)

                // Call the ENROLL endpoint
                val response = ApiClient.service.enrollUser(body)
                if (response.isSuccessful) {
                    onResult("✅ Enrollment Success!")
                } else {
                    onResult("❌ Server Error")
                }
            } catch (e: Exception) {
                onResult("❌ Connection Failed")
            }
        }
    }
}