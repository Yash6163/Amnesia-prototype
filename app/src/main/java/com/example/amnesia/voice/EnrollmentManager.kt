package com.example.amnesia.voice

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.example.amnesia.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class EnrollmentManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var startTime: Long = 0

    fun startEnrollment() {
        audioFile = File(context.cacheDir, "enroll.m4a")
        if (audioFile?.exists() == true) audioFile?.delete()

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(16000) // Match server
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            Log.d("Enrollment", "✅ Recorder Started")
        } catch (e: Exception) {
            Log.e("Enrollment", "❌ Failed to start", e)
        }
    }

    fun stopAndUpload(role: String, onResult: (String) -> Unit) {
        // ✅ SAFETY CHECK: Ensure we recorded at least 1.5 seconds
        val duration = System.currentTimeMillis() - startTime
        if (duration < 1500) {
            CoroutineScope(Dispatchers.IO).launch {
                delay(1500 - duration) // Wait until 1.5s passes
                stopAndUploadInternal(role, onResult)
            }
        } else {
            stopAndUploadInternal(role, onResult)
        }
    }

    private fun stopAndUploadInternal(role: String, onResult: (String) -> Unit) {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            // -1007 happens if file is empty.
            Log.e("Enrollment", "Stop failed (probably empty)", e)
        }
        mediaRecorder = null

        val file = audioFile
        if (file == null || !file.exists() || file.length() < 100) {
            onResult("❌ Audio too short/empty. Try again.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)
                val rolePart = MultipartBody.Part.createFormData("role", role)

                val response = ApiClient.service.enrollUser(body, rolePart)

                if (response.isSuccessful) {
                    onResult("✅ Success!")
                } else {
                    onResult("❌ Server Error: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult("❌ Connection/Timeout Error")
            }
        }
    }
}