package com.example.myapplication

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
