package com.example.amnesia.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// ✅ UPDATED to match your new Python Server JSON
data class ProcessResponse(
    val success: Boolean,        // Was 'authorized', now 'success'
    val role: String?,           // 'patient', 'caregiver', or 'unknown'
    val text: String?,           // Transcribed text
    val is_repeat: Boolean?,     // True if patient repeated a question
    val display_message: String? // The nice message your server generates
)

data class EnrollResponse(
    val message: String
)

interface ApiService {
    @Multipart
    @POST("enroll")
    suspend fun enrollUser(
        @Part audio: MultipartBody.Part,
        @Part role: MultipartBody.Part
    ): Response<EnrollResponse>

    @Multipart
    @POST("process")
    suspend fun processAudio(
        @Part audio: MultipartBody.Part
    ): Response<ProcessResponse>
}