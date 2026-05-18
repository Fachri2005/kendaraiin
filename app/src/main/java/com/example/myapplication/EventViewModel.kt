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

    /**
     * SINKRONISASI: Mengambil data dari API dan menyimpannya ke SQLite
     */
    fun fetchEventsFromApi(adminEmail: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            // 1. Tampilkan data dari Local DB dulu (biar user gak nunggu loading lama)
            val localData = withContext(Dispatchers.IO) {
                repository.getAllEventsFromLocal()
            }
            if (localData.isNotEmpty()) {
                _events.postValue(localData)
            }

            // 2. Ambil data terbaru dari Server (XAMPP)
            try {
                val response = repository.getEventsFromApi(adminEmail)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val remoteData = apiResponse.data ?: emptyList()

                        // 3. Simpan data terbaru ke SQLite (Sinkronisasi)
                        withContext(Dispatchers.IO) {
                            repository.saveEventsToLocal(remoteData)
                        }

                        // 4. Update UI dengan data hasil sinkronisasi
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

    fun addEvent(event: Event, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.addEventToApi(event)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    // Refresh dan sinkron ulang
                    fetchEventsFromApi(event.adminEmail)
                } else {
                    _error.postValue("Gagal menambah unit: ${response.body()?.message}")
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
