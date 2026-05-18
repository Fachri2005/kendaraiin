package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class Event(
    val id: Int = 0,
    val name: String,
    val price: String,
    val description: String,
    @SerializedName("is_registered")
    val isRegistered: Boolean = false, // Ubah menjadi Boolean agar cocok dengan API
    @SerializedName("admin_email")
    val adminEmail: String? = null,
    @SerializedName("image_uri")
    val imageUri: String? = null,
    @SerializedName("vehicle_type")
    val vehicleType: String? = null,
    val transmission: String? = "Matic",
    val seats: String? = "5 Kursi",
    val location: String? = "Jakarta",
    @SerializedName("renter_email")
    val renterEmail: String? = null
)
