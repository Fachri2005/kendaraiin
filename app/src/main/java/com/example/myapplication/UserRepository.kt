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
        try {
            val response = apiService.register("register", user)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e("UserRepository", "Register Error", e)
            false
        }
    }

    suspend fun loginUser(email: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login("login", email, password)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) body.data else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Login Error", e)
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserByEmail("profile", email)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "GetUserByEmail Error", e)
            null
        }
    }

    suspend fun updateUser(email: String, name: String, phone: String, imageUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateUser("update_profile", email, name, phone, imageUri)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e("UserRepository", "UpdateUser Error", e)
            false
        }
    }

    suspend fun changePassword(email: String, currentPass: String, newPass: String): ApiResponse<Unit>? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.changePassword("change_password", email, currentPass, newPass)
            response.body()
        } catch (e: Exception) {
            Log.e("UserRepository", "ChangePassword Error", e)
            null
        }
    }

    suspend fun deleteAccount(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteAccount("delete_account", email)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e("UserRepository", "DeleteAccount Error", e)
            false
        }
    }

    suspend fun saveLoginSession(user: User) = withContext(Dispatchers.IO) {
        sharedPref.edit(commit = true) {
            putBoolean("is_logged_in", true)
            putInt("user_id", user.id)
            putString("user_name", user.name)
            putString("user_email", user.email)
            putString("user_role", user.role)
            putString("user_phone", user.phone)
            putString("user_image_uri", user.imageUri)
            putString("user_gender", user.gender)
            putString("user_birth_date", user.birthDate)
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
    fun getUserPhone(): String? = sharedPref.getString("user_phone", null)
    fun getUserImageUri(): String? = sharedPref.getString("user_image_uri", null)

    fun getFullUser(): User? {
        if (!isLoggedIn()) return null
        return User(
            id = sharedPref.getInt("user_id", 0),
            name = sharedPref.getString("user_name", "") ?: "",
            email = sharedPref.getString("user_email", "") ?: "",
            password = null,
            role = sharedPref.getString("user_role", "") ?: "",
            phone = sharedPref.getString("user_phone", "") ?: "",
            imageUri = sharedPref.getString("user_image_uri", "") ?: "",
            gender = sharedPref.getString("user_gender", "") ?: "",
            birthDate = sharedPref.getString("user_birth_date", "") ?: ""
        )
    }
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
