package com.example.myapplication

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("events") // Placeholder endpoint
    suspend fun getEvents(): Response<List<Event>>
}
