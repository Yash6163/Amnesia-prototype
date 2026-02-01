package com.example.amnesia.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // ✅ REMOVED 'private' so SpeechManager can read it
    // ✅ KEEP 127.0.0.1 because you ran 'adb reverse'
    const val BASE_URL = "http://127.0.0.1:5001/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}