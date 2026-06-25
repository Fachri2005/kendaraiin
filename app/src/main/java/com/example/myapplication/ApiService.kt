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

    // Auth API
    @POST("auth.php")
    suspend fun register(
        @Query("action") action: String,
        @Body user: User
    ): Response<ApiResponse<Unit>>

    @FormUrlEncoded
    @POST("auth.php")
    suspend fun login(
        @Query("action") action: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<ApiResponse<User>>

    @GET("auth.php")
    suspend fun getUserByEmail(
        @Query("action") action: String,
        @Query("email") email: String
    ): Response<ApiResponse<User>>

    @FormUrlEncoded
    @POST("auth.php")
    suspend fun updateUser(
        @Query("action") action: String,
        @Field("email") email: String,
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("image_uri") imageUri: String
    ): Response<ApiResponse<Unit>>

    @FormUrlEncoded
    @POST("auth.php")
    suspend fun changePassword(
        @Query("action") action: String,
        @Field("email") email: String,
        @Field("current_password") currentPass: String,
        @Field("new_password") newPass: String
    ): Response<ApiResponse<Unit>>

    // Hapus Akun
    @FormUrlEncoded
    @POST("auth.php")
    suspend fun deleteAccount(
        @Query("action") action: String,
        @Field("email") email: String
    ): Response<ApiResponse<Unit>>

    // Rental API
    @FormUrlEncoded
    @POST("events.php")
    suspend fun rentVehicle(
        @Query("action") action: String,
        @Field("id") id: Int,
        @Field("renter_email") renter_email: String,
        @Field("start_date") start_date: String,
        @Field("duration") duration: Int,
        @Field("pickup_location") pickup_location: String
    ): Response<ApiResponse<Unit>>

    @FormUrlEncoded
    @POST("events.php")
    suspend fun updateRentalStatus(
        @Query("action") action: String,
        @Field("id") id: Int,
        @Field("rental_status") rentalStatus: String,
        @Field("status") status: String,
        @Field("is_registered") isRegistered: Int
    ): Response<ApiResponse<Unit>>
}
