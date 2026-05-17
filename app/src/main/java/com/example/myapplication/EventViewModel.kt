package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableCorner
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
                val response = RetrofitClient.apiService.getEvents()
                if (response.isSuccessful) {
                    _events.postValue(response.body())
                    _error.postValue(null)
                } else {
                    _error.postValue("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue("Failure: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    
    fun loadLocalEvents(adminEmail: String? = null) {
        viewModelScope.launch {
            val localEvents = if (adminEmail != null) {
                repository.getEventsByAdmin(adminEmail)
            } else {
                repository.getAllEvents()
            }
            _events.postValue(localEvents)
        }
    }
}
