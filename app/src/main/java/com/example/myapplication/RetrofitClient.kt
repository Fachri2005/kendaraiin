package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    /**
     * PENTING: Ganti IP di bawah ini dengan IPv4 Address laptop Anda (cek di cmd: ipconfig).
     * Jika pakai Hotspot HP, biasanya 192.168.43.x atau 192.168.137.1
     */
    private const val BASE_URL = "http://192.168.137.1/event_api/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
