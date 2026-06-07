package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventViewModel(private val repository: EventRepository) : ViewModel() {

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _rentalSuccess = MutableLiveData<Boolean>()
    val rentalSuccess: LiveData<Boolean> get() = _rentalSuccess

    fun fetchEventsFromApi(adminEmail: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            val localData = withContext(Dispatchers.IO) {
                repository.getAllEventsFromLocal()
            }
            if (localData.isNotEmpty()) {
                _events.postValue(localData)
            }

            try {
                val response = repository.getEventsFromApi(adminEmail)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val remoteData = apiResponse.data ?: emptyList()
                        withContext(Dispatchers.IO) {
                            repository.saveEventsToLocal(remoteData)
                        }
                        _events.postValue(remoteData)
                        _error.postValue(null)
                    } else {
                        _error.postValue(apiResponse?.message ?: "Gagal sinkron data")
                    }
                } else {
                    _error.postValue("Server Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.postValue("Mode Offline: Gagal terhubung ke server")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun rentVehicle(id: Int, renterEmail: String, startDate: String, duration: Int, pickup: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Gunakan nama parameter dan tipe data yang sesuai dengan ApiService yang baru
                val response = repository.apiService.rentVehicle(
                    action = "rent",
                    id = id,                     // Mengirim Int
                    renter_email = renterEmail,
                    start_date = startDate,      // Nama parameter baru
                    duration = duration,         // Mengirim Int & Nama parameter baru
                    pickup_location = pickup
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    withContext(Dispatchers.IO) {
                        repository.rentVehicleLocal(id, renterEmail, startDate, duration, pickup)
                    }
                    fetchEventsFromApi() 
                    _rentalSuccess.postValue(true)
                } else {
                    val errorMsg = response.body()?.message ?: "Gagal menyimpan ke server"
                    _error.postValue("Gagal menyewa: $errorMsg")
                }
            } catch (e: Exception) {
                _error.postValue("Gagal terhubung ke server: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun addEvent(event: Event, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.addEventToApi(event)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    fetchEventsFromApi(event.adminEmail)
                } else {
                    _error.postValue("Gagal menambah unit: ${response.body()?.message ?: "Unknown Error"}")
                }
            } catch (e: Exception) {
                _error.postValue("Gagal: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateEvent(id: Int, event: Event, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.updateEventToApi(id, event)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    fetchEventsFromApi(event.adminEmail)
                } else {
                    _error.postValue("Gagal update: ${response.body()?.message ?: "Unknown Error"}")
                }
            } catch (e: Exception) {
                _error.postValue("Gagal: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteEvent(id: Int, adminEmail: String?, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.deleteEventFromApi(id, adminEmail)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    fetchEventsFromApi(adminEmail)
                } else {
                    _error.postValue("Gagal menghapus: ${response.body()?.message ?: "Unknown Error"}")
                }
            } catch (e: Exception) {
                _error.postValue("Gagal: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun searchVehicles(query: String) {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                repository.searchVehicles(query)
            }
            _events.postValue(results)
        }
    }
}
