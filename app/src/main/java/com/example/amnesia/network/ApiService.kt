package com.example.amnesia.network

// --- ADD THESE IMPORTS ---
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
// -------------------------

data class ProcessResponse(
    val authorized: Boolean,
    val text: String?,
    val message: String?
)

data class EnrollResponse(
    val message: String
)

interface ApiService {
    @Multipart
    @POST("enroll")
    suspend fun enrollUser(
        @Part audio: MultipartBody.Part
    ): Response<EnrollResponse>

    @Multipart
    @POST("process")
    suspend fun processAudio(
        @Part audio: MultipartBody.Part
    ): Response<ProcessResponse>
}