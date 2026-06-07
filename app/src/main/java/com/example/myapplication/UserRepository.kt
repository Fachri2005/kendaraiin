package com.example.myapplication

import android.content.Context
import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    context: Context,
    private val apiService: ApiService = RetrofitClient.apiService
) {
    private val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    suspend fun registerUser(user: User): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.register("register", user)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    true
                } else {
                    Log.e("API_ERROR", "Registrasi Gagal: ${body?.message}")
                    false
                }
            } else {
                Log.e("API_ERROR", "HTTP Error Register: ${response.code()} - ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Koneksi Register Gagal: ${e.message}")
            false
        }
    }

    suspend fun loginUser(email: String, password: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.login("login", email, password)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    body.data
                } else {
                    Log.e("API_ERROR", "Login Gagal: ${body?.message}")
                    null
                }
            } else {
                Log.e("API_ERROR", "HTTP Error Login: ${response.code()} - ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Koneksi Login Gagal: ${e.message}")
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.getUserByEmail("profile", email)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data
            } else {
                Log.e("API_ERROR", "Fetch Profile Gagal: ${response.body()?.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Koneksi Profile Gagal: ${e.message}")
            null
        }
    }

    suspend fun updateUser(email: String, name: String, phone: String, imageUri: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.updateUser("update_profile", email, name, phone, imageUri)
            if (response.isSuccessful && response.body()?.success == true) {
                true
            } else {
                Log.e("API_ERROR", "Update Profile Gagal: ${response.body()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Koneksi Update Gagal: ${e.message}")
            false
        }
    }

    fun saveLoginSession(user: User) {
        sharedPref.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", user.name)
            putString("user_email", user.email)
            putString("user_role", user.role)
            apply()
        }
    }

    fun logout() {
        sharedPref.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = sharedPref.getBoolean("is_logged_in", false)
    fun getUserEmail(): String? = sharedPref.getString("user_email", null)
    fun getUserName(): String? = sharedPref.getString("user_name", null)
    fun getUserRole(): String? = sharedPref.getString("user_role", null)
}

data class User(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String,
    @SerializedName("phone") val phone: String = "",
    @SerializedName("emergency_phone") val emergencyPhone: String = "",
    @SerializedName("gender") val gender: String = "",
    @SerializedName("birth_date") val birthDate: String = "",
    @SerializedName("image_uri") val imageUri: String = ""
)
