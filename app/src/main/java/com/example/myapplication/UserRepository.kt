package com.example.myapplication

import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class UserRepository(
    context: Context,
    private val apiService: ApiService = RetrofitClient.apiService
) {
    // dbHelper tetap dipertahankan jika Anda ingin caching lokal di masa depan
    private val dbHelper = EventDatabaseHelper(context)

    suspend fun registerUser(user: User): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.register(user)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun loginUser(email: String, password: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.login(email, password)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        // Implementasi ambil data user spesifik dari API jika diperlukan
        null
    }

    suspend fun updateUser(email: String, name: String, phone: String, imageUri: String): Int = withContext(Dispatchers.IO) {
        // Implementasi update user ke API
        0
    }
}

data class User(
    @SerializedName("id") 
    val id: Int = 0,
    
    @SerializedName("name") 
    val name: String,
    
    @SerializedName("email") 
    val email: String,
    
    @SerializedName("password") 
    val password: String,
    
    @SerializedName("role") 
    val role: String,
    
    @SerializedName("phone") 
    val phone: String = "",
    
    @SerializedName("emergency_phone") 
    val emergencyPhone: String = "",
    
    @SerializedName("gender") 
    val gender: String = "",
    
    @SerializedName("birth_date") 
    val birthDate: String = "",
    
    @SerializedName("image_uri") 
    val imageUri: String = ""
)
