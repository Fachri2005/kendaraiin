package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class EventViewModel(private val repository: EventRepository) : ViewModel() {

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchEventsFromApi() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getEventsFromApi()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _events.postValue(apiResponse.data ?: emptyList())
                        _error.postValue(null)
                    } else {
                        _error.postValue(apiResponse?.message ?: "Gagal mengambil data")
                    }
                } else {
                    _error.postValue("Server Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.postValue("Koneksi Gagal: ${e.localizedMessage}")
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
                    fetchEventsFromApi()
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

    // Fungsi pencarian baru: Memfilter list yang sudah ada di memori
    fun searchLocalList(query: String, adminEmail: String?) {
        val currentList = _events.value ?: return
        val filtered = currentList.filter { 
            it.name.contains(query, ignoreCase = true) && 
            (adminEmail == null || it.adminEmail?.trim()?.equals(adminEmail.trim(), ignoreCase = true) == true)
        }
        // Jangan timpa _events utama agar bisa kembali saat query kosong
        // Tapi untuk kesederhanaan di HomeFragment, kita gunakan filter di level UI saja.
    }

    fun updateEvent(id: Int, event: Event, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.updateEventToApi(id, event)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    fetchEventsFromApi()
                }
            } catch (e: Exception) {
                _error.postValue("Gagal: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteEvent(id: Int, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.deleteEventFromApi(id)
                if (response.isSuccessful && response.body()?.success == true) {
                    onSuccess()
                    fetchEventsFromApi()
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
            val results = repository.searchVehicles(query)
            _events.postValue(results)
        }
    }
}
