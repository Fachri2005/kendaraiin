package com.example.myapplication

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("events.php")
    suspend fun getEvents(
        @Query("admin_email") adminEmail: String? = null
    ): Response<ApiResponse<List<Event>>>

    @GET("events.php")
    suspend fun getEventById(@Query("id") id: Int): Response<ApiResponse<Event>>

    @POST("events.php")
    suspend fun addEvent(@Body event: Event): Response<ApiResponse<Map<String, Int>>>

    @PUT("events.php")
    suspend fun updateEvent(@Query("id") id: Int, @Body event: Event): Response<ApiResponse<Unit>>

    @DELETE("events.php")
    suspend fun deleteEvent(
        @Query("id") id: Int,
        @Query("admin_email") adminEmail: String? = null
    ): Response<ApiResponse<Unit>>

    // Tambahkan untuk Auth (XAMPP)
    @POST("auth.php?action=register")
    suspend fun register(@Body user: User): Response<ApiResponse<Unit>>

    @FormUrlEncoded
    @POST("auth.php?action=login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<ApiResponse<User>>
}
