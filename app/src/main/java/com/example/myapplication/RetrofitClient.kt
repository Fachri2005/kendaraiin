package com.example.myapplication

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Gunakan 10.0.2.2 agar emulator bisa memanggil localhost laptop
    private const val BASE_URL = "http://10.128.116.140/event_api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Menggunakan HEADERS alih-alih BODY untuk menghindari 'unexpected end of stream'
        // saat koneksi lokal tidak stabil atau ada PHP warning di tengah output.
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // Memaksa HTTP/1.1 untuk menghindari masalah stream pada server lokal (seperti XAMPP)
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
