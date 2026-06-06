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

    fun loginAsGuest() {
        val guestUser = User(name = "Tamu", email = "guest@kendaraiin.com", password = "", role = "Customer")
        repository.saveLoginSession(guestUser)
        _user.value = guestUser
    }

    fun isLoggedIn(): Boolean = repository.isLoggedIn()
}
