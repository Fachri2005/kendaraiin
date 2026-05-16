package com.example.myapplication

data class Event(
    val id: Int = 0,
    val name: String,
    val price: String,
    val description: String,
    var isRegistered: Boolean = false,
    val adminEmail: String? = null,
    val imageUri: String? = null,
    val vehicleType: String? = null,
    val transmission: String? = "Matic",
    val seats: String? = "5 Kursi",
    val location: String? = "Jakarta",
    val renterEmail: String? = null
)