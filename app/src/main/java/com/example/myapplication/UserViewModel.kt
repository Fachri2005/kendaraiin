package com.example.myapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _isSuccess = MutableLiveData<EventWrapper<Boolean>>()
    val isSuccess: LiveData<EventWrapper<Boolean>> get() = _isSuccess

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<EventWrapper<Int?>>()
    val error: LiveData<EventWrapper<Int?>> get() = _error

    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.loginUser(email, password)
                if (result != null) {
                    repository.saveLoginSession(result)
                    _user.postValue(result)
                    _error.postValue(EventWrapper(null))
                } else {
                    _error.postValue(EventWrapper(R.string.error_login_failed))
                }
            } catch (e: IOException) {
                Log.e("UserViewModel", "Network error during login", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: HttpException) {
                Log.e("UserViewModel", "HTTP error during login", e)
                _error.postValue(EventWrapper(R.string.error_server_unavailable))
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unexpected error during login", e)
                _error.postValue(EventWrapper(R.string.error_login_failed))
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
                    _isSuccess.postValue(EventWrapper(true))
                    _error.postValue(EventWrapper(null))
                } else {
                    _error.postValue(EventWrapper(R.string.error_register_failed))
                }
            } catch (e: IOException) {
                Log.e("UserViewModel", "Network error during register", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unexpected error during register", e)
                _error.postValue(EventWrapper(R.string.error_register_failed))
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
                Log.e("UserViewModel", "Error fetching user", e)
                _error.postValue(EventWrapper(R.string.error_data_not_found))
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
                    _isSuccess.postValue(EventWrapper(true))
                    val currentUser = _user.value
                    if (currentUser != null) {
                        val updatedUser = currentUser.copy(name = name, phone = phone, imageUri = imageUri)
                        repository.saveLoginSession(updatedUser)
                        _user.postValue(updatedUser)
                    }
                } else {
                    _error.postValue(EventWrapper(R.string.error_data_not_found))
                }
            } catch (e: IOException) {
                Log.e("UserViewModel", "Network error during updateUser", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unexpected error during updateUser", e)
                _error.postValue(EventWrapper(R.string.error_data_not_found))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun changePassword(email: String, currentPass: String, newPass: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.changePassword(email, currentPass, newPass)
                if (response?.success == true) {
                    _isSuccess.postValue(EventWrapper(true))
                    _error.postValue(EventWrapper(null))
                } else {
                    _error.postValue(EventWrapper(R.string.error_current_password_wrong))
                }
            } catch (e: IOException) {
                Log.e("UserViewModel", "Network error during changePassword", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unexpected error during changePassword", e)
                _error.postValue(EventWrapper(R.string.error_data_not_found))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteAccount(email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = repository.deleteAccount(email)
                if (success) {
                    repository.logout()
                    _isSuccess.postValue(EventWrapper(true))
                } else {
                    _error.postValue(EventWrapper(R.string.error_data_not_found))
                }
            } catch (e: IOException) {
                Log.e("UserViewModel", "Network error during deleteAccount", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("UserViewModel", "Unexpected error during deleteAccount", e)
                _error.postValue(EventWrapper(R.string.error_data_not_found))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            val guestUser = User(name = "Tamu", email = "guest@kendaraiin.com", password = "", role = "Customer")
            repository.saveLoginSession(guestUser)
            _user.postValue(guestUser)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _user.postValue(null)
        }
    }

    fun isLoggedIn(): Boolean = repository.isLoggedIn()
    fun getUserEmail(): String? = repository.getUserEmail()
    fun getUserName(): String? = repository.getUserName()
    fun getUserRole(): String? = repository.getUserRole()
}
