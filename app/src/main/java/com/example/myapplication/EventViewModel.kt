package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class EventViewModel(
    private val repository: EventRepository,
    private val userRepository: UserRepository,
    context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private val notificationHelper = NotificationHelper(appContext)

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<EventWrapper<Int?>>()
    val error: LiveData<EventWrapper<Int?>> get() = _error

    private val _rentalSuccess = MutableLiveData<EventWrapper<Boolean>>()
    val rentalSuccess: LiveData<EventWrapper<Boolean>> get() = _rentalSuccess

    fun fetchEventsFromApi(adminEmail: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            val localData = withContext(Dispatchers.IO) {
                repository.getAllEventsFromLocal()
            }
            _events.value = localData

            try {
                val response = repository.getEventsFromApi(adminEmail)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val remoteData = apiResponse.data ?: emptyList()
                        
                        val userEmail = userRepository.getUserEmail()
                        val userRole = userRepository.getUserRole()

                        checkAndNotify(localData, remoteData, userEmail, userRole)

                        val syncedList = remoteData.map { remoteEvent ->
                            val localMatch = localData.find { it.id == remoteEvent.id }
                            
                            remoteEvent.copy(
                                renterEmail = if (remoteEvent.renterEmail.isNullOrEmpty()) {
                                    localMatch?.renterEmail
                                } else {
                                    remoteEvent.renterEmail
                                },
                                rentalStatus = if (remoteEvent.rentalStatus.isNullOrEmpty()) {
                                    localMatch?.rentalStatus
                                } else {
                                    remoteEvent.rentalStatus
                                }
                            )
                        }

                        withContext(Dispatchers.IO) {
                            repository.saveEventsToLocal(syncedList)
                        }
                        _events.postValue(syncedList)
                    } else {
                        _error.postValue(EventWrapper(R.string.error_data_not_found))
                    }
                } else {
                    _error.postValue(EventWrapper(R.string.error_data_not_found))
                }
            } catch (e: Exception) {
                Log.e("API_SYNC", "Error: ${e.message}")
                _error.postValue(EventWrapper(R.string.error_data_not_found))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun checkAndNotify(localData: List<Event>, remoteData: List<Event>, userEmail: String?, userRole: String?) {
        val isEnabled = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)
        if (!isEnabled) return

        if (userRole == "Admin") {
            val newPendingCount = remoteData.count { remote ->
                val isNewPending = remote.rentalStatus == "pending"
                val wasNotPending = localData.find { it.id == remote.id }?.rentalStatus != "pending"
                isNewPending && wasNotPending
            }
            if (newPendingCount > 0) {
                notificationHelper.showNotification(
                    99,
                    appContext.getString(R.string.notification_new_rental_title),
                    appContext.getString(R.string.notification_new_rental_desc, newPendingCount)
                )
            }
        } else if (userRole == "Customer" && userEmail != null) {
            val sharedPref = appContext.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
            val seenIds = sharedPref.getStringSet("seen_approved_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            var hasNewNotification = false

            remoteData.forEach { remote ->
                if (remote.renterEmail == userEmail) {
                    val localMatch = localData.find { it.id == remote.id }
                    val isNewlyApproved = remote.rentalStatus == "approved" && localMatch?.rentalStatus != "approved"
                    val isNotSeenYet = !seenIds.contains(remote.id.toString())

                    if (isNewlyApproved && isNotSeenYet) {
                        notificationHelper.showNotification(
                            remote.id,
                            appContext.getString(R.string.notification_approved_title),
                            appContext.getString(R.string.notification_approved_desc, remote.name)
                        )
                        seenIds.add(remote.id.toString())
                        hasNewNotification = true
                    }
                }
            }
            
            if (hasNewNotification) {
                sharedPref.edit().putStringSet("seen_approved_ids", seenIds).apply()
            }
        }
    }

    fun updateRentalStatus(id: Int, status: String, adminEmail: String? = null) {
        val targetStatus = status.lowercase().trim()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = repository.apiService.updateRentalStatus(
                    action = "update_status",
                    id = id,
                    rentalStatus = targetStatus,
                    status = targetStatus,
                    isRegistered = if (targetStatus == "approved") 1 else 0
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    withContext(Dispatchers.IO) {
                        repository.updateRentalStatusLocal(id, targetStatus)
                    }
                    delay(500)
                    fetchEventsFromApi(adminEmail)
                } else {
                    _error.postValue(EventWrapper(R.string.error_data_not_found))
                }
            } catch (e: Exception) {
                _error.postValue(EventWrapper(R.string.error_data_not_found))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun rentVehicle(id: Int, renterEmail: String, startDate: String, duration: Int, pickup: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.apiService.rentVehicle("rent", id, renterEmail, startDate, duration, pickup)
                if (response.isSuccessful && response.body()?.success == true) {
                    withContext(Dispatchers.IO) { repository.rentVehicleLocal(id, renterEmail, startDate, duration, pickup) }
                    fetchEventsFromApi()
                    _rentalSuccess.postValue(EventWrapper(true))
                } else {
                    _error.postValue(EventWrapper(R.string.error_data_not_found))
                }
            } catch (e: Exception) {
                _error.postValue(EventWrapper(R.string.error_data_not_found))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun addEvent(e: Event, cb: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.addEventToApi(e)
                if (response.isSuccessful && response.body()?.success == true) { 
                    cb(); fetchEventsFromApi(e.adminEmail) 
                } else { _error.postValue(EventWrapper(R.string.error_data_not_found)) }
            } catch (e: Exception) { _error.postValue(EventWrapper(R.string.error_data_not_found)) } finally { _isLoading.postValue(false) }
        }
    }

    fun deleteEvent(id: Int, email: String?, cb: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.deleteEventFromApi(id, email)
                if (response.isSuccessful && response.body()?.success == true) { 
                    cb(); fetchEventsFromApi(email) 
                } else { _error.postValue(EventWrapper(R.string.error_data_not_found)) }
            } catch (e: Exception) { _error.postValue(EventWrapper(R.string.error_data_not_found)) } finally { _isLoading.postValue(false) }
        }
    }

    fun updateEvent(id: Int, e: Event, cb: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.updateEventToApi(id, e)
                if (response.isSuccessful && response.body()?.success == true) { 
                    cb(); fetchEventsFromApi(e.adminEmail) 
                } else { _error.postValue(EventWrapper(R.string.error_data_not_found)) }
            } catch (e: Exception) { _error.postValue(EventWrapper(R.string.error_data_not_found)) } finally { _isLoading.postValue(false) }
        }
    }

    fun searchVehicles(q: String) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { repository.searchVehicles(q) }
            _events.postValue(r)
        }
    }
}
