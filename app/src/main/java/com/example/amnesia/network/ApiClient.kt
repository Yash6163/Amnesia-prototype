package com.example.amnesia.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // REPLACE THIS WITH YOUR LAPTOP'S IP ADDRESS
    // Keep http:// and :5000/
    private const val BASE_URL = "http://10.171.33.166:5001/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}