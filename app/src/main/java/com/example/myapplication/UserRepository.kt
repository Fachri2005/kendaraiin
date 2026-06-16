package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    context: Context,
    private val apiService: ApiService = RetrofitClient.apiService
) {
    private val appContext = context.applicationContext
    private val sharedPref = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    suspend fun registerUser(user: User): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.register("register", user)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e("UserRepository", "Register error", e)
            false
        }
    }

    suspend fun loginUser(email: String, password: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.login("login", email, password)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) body.data else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Login error", e)
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.getUserByEmail("profile", email)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(email: String, name: String, phone: String, imageUri: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.updateUser("update_profile", email, name, phone, imageUri)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun changePassword(email: String, currentPass: String, newPass: String): ApiResponse<Unit>? = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.changePassword("change_password", email, currentPass, newPass)
            response.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteAccount(email: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.deleteAccount("delete_account", email)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveLoginSession(user: User) = withContext(Dispatchers.IO) {
        sharedPref.edit(commit = true) {
            putBoolean("is_logged_in", true)
            putString("user_name", user.name)
            putString("user_email", user.email)
            putString("user_role", user.role)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sharedPref.edit(commit = true) {
            clear()
            putBoolean("is_logged_in", false)
        }
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
    @SerializedName("password") val password: String? = null,
    @SerializedName("role") val role: String,
    @SerializedName("phone") val phone: String = "",
    @SerializedName("emergency_phone") val emergencyPhone: String = "",
    @SerializedName("gender") val gender: String = "",
    @SerializedName("birth_date") val birthDate: String = "",
    @SerializedName("image_uri") val imageUri: String = "",
    @SerializedName("rating") val rating: Double? = 0.0,
    @SerializedName("total_review") val totalReview: Int? = 0
)
