package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _isSuccess = MutableLiveData<Boolean>()
    val isSuccess: LiveData<Boolean> get() = _isSuccess

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.loginUser(email, password)
                if (result != null) {
                    repository.saveLoginSession(result)
                    _user.postValue(result)
                    _error.postValue(null)
                } else {
                    _error.postValue("Login Gagal! Cek Email/Password atau Koneksi Server.")
                }
            } catch (e: Exception) {
                _error.postValue("Terjadi kesalahan: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun register(user: User) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = repository.registerUser(user)
                if (success) {
                    repository.saveLoginSession(user)
                    _isSuccess.postValue(true)
                    _error.postValue(null)
                } else {
                    _error.postValue("Registrasi Gagal! Email mungkin sudah terdaftar.")
                }
            } catch (e: Exception) {
                _error.postValue("Terjadi kesalahan: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchUser(email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getUserByEmail(email)
                _user.postValue(result)
            } catch (e: Exception) {
                _error.postValue("Gagal mengambil data: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateUser(email: String, name: String, phone: String, imageUri: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = repository.updateUser(email, name, phone, imageUri)
                if (success) {
                    _isSuccess.postValue(true)
                    // Update local session
                    val currentUser = _user.value
                    if (currentUser != null) {
                        val updatedUser = currentUser.copy(name = name, phone = phone, imageUri = imageUri)
                        repository.saveLoginSession(updatedUser)
                        _user.postValue(updatedUser)
                    }
                } else {
                    _error.postValue("Gagal memperbarui profil.")
                }
            } catch (e: Exception) {
                _error.postValue("Terjadi kesalahan: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loginAsGuest() {
        val guestUser = User(name = "Tamu", email = "guest@kendaraiin.com", password = "", role = "Customer")
        repository.saveLoginSession(guestUser)
        _user.value = guestUser
    }

    fun logout() {
        repository.logout()
        _user.value = null
    }

    fun isLoggedIn(): Boolean = repository.isLoggedIn()
    fun getUserEmail(): String? = repository.getUserEmail()
    fun getUserName(): String? = repository.getUserName()
    fun getUserRole(): String? = repository.getUserRole()
}
