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
import java.net.ProtocolException
import java.util.concurrent.CancellationException

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

    fun fetchEventsFromApi(adminEmail: String? = null, forceRefresh: Boolean = false) {
        if (!forceRefresh && _isLoading.value == true) return
        
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val localData = repository.getAllEventsFromLocal()
                if (!forceRefresh) _events.value = localData

                val response = repository.getEventsFromApi(adminEmail)
                if (response.isSuccessful && response.body()?.success == true) {
                    val remoteData = response.body()?.data ?: emptyList()
                    val userEmail = userRepository.getUserEmail()
                    val userRole = userRepository.getUserRole()

                    checkAndNotify(localData, remoteData, userEmail, userRole)

                    val syncedList = withContext(Dispatchers.Default) {
                        val localMap = localData.associateBy { it.id }
                        remoteData.map { remoteEvent ->
                            val localMatch = localMap[remoteEvent.id]
                            remoteEvent.copy(
                                renterEmail = if (remoteEvent.renterEmail.isNullOrEmpty()) localMatch?.renterEmail else remoteEvent.renterEmail,
                                rentalStatus = if (remoteEvent.rentalStatus.isNullOrEmpty()) localMatch?.rentalStatus else remoteEvent.rentalStatus
                            )
                        }
                    }

                    repository.saveEventsToLocal(syncedList)
                    _events.postValue(syncedList)
                } else {
                    Log.e("EventViewModel", "API Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: ProtocolException) {
                Log.e("EventViewModel", "Protocol Error (Unexpected end of stream): ${e.message}")
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: CancellationException) {
                Log.d("EventViewModel", "Job cancelled")
            } catch (e: Exception) {
                Log.e("EventViewModel", "Sync Error", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun checkAndNotify(localData: List<Event>, remoteData: List<Event>, userEmail: String?, userRole: String?) = withContext(Dispatchers.Default) {
        val isEnabled = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getBoolean("notifications_enabled", true)
        if (!isEnabled) return@withContext

        val localMap = localData.associateBy { it.id }

        if (userRole == "Admin") {
            val count = remoteData.count { r -> r.rentalStatus == "pending" && localMap[r.id]?.rentalStatus != "pending" }
            if (count > 0) {
                withContext(Dispatchers.Main) {
                    notificationHelper.showNotification(99, appContext.getString(R.string.notification_new_rental_title), appContext.getString(R.string.notification_new_rental_desc, count))
                }
            }
        } else if (userRole == "Customer" && userEmail != null) {
            remoteData.forEach { r ->
                if (r.renterEmail == userEmail && r.rentalStatus == "approved" && localMap[r.id]?.rentalStatus != "approved") {
                    withContext(Dispatchers.Main) {
                        notificationHelper.showNotification(r.id, appContext.getString(R.string.notification_approved_title), appContext.getString(R.string.notification_approved_desc, r.name ?: ""))
                    }
                }
            }
        }
    }

    fun updateEvent(id: Int, e: Event, cb: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.updateEventToApi(id, e)
                if (response.isSuccessful && response.body()?.success == true) {
                    delay(300)
                    fetchEventsFromApi(e.adminEmail, forceRefresh = true)
                    withContext(Dispatchers.Main) { cb() }
                } else {
                    _error.postValue(EventWrapper(R.string.error_data_not_found))
                }
            } catch (e: ProtocolException) {
                Log.e("EventViewModel", "Protocol Error: ${e.message}")
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("EventViewModel", "Update Error", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateRentalStatus(id: Int, status: String, adminEmail: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val s = status.lowercase().trim()
                // Logika isRegistered: 1 jika sedang disewa atau menunggu persetujuan
                val isReg = if (s == "approved" || s == "pending") 1 else 0
                
                val response = repository.apiService.updateRentalStatus("update_status", id, s, s, isReg)
                if (response.isSuccessful && response.body()?.success == true) {
                    repository.updateRentalStatusLocal(id, s)
                    delay(300)
                    fetchEventsFromApi(adminEmail, forceRefresh = true)
                } else {
                    _error.postValue(EventWrapper(R.string.error_server_unavailable))
                }
            } catch (e: ProtocolException) {
                Log.e("EventViewModel", "Protocol Error in Update Status: ${e.message}")
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("EventViewModel", "Update Status Error", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } finally { 
                _isLoading.postValue(false) 
            }
        }
    }

    fun rentVehicle(id: Int, email: String, date: String, dur: Int, pick: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val res = repository.apiService.rentVehicle("rent", id, email, date, dur, pick)
                if (res.isSuccessful && res.body()?.success == true) {
                    repository.rentVehicleLocal(id, email, date, dur, pick)
                    fetchEventsFromApi(forceRefresh = true)
                    _rentalSuccess.postValue(EventWrapper(true))
                } else {
                    _error.postValue(EventWrapper(R.string.error_server_unavailable))
                }
            } catch (e: ProtocolException) {
                Log.e("EventViewModel", "Protocol Error in Rent: ${e.message}")
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("EventViewModel", "Rent Error", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } finally { 
                _isLoading.postValue(false) 
            }
        }
    }

    fun addEvent(e: Event, cb: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val res = repository.addEventToApi(e)
                if (res.isSuccessful && res.body()?.success == true) {
                    fetchEventsFromApi(e.adminEmail, forceRefresh = true)
                    withContext(Dispatchers.Main) { cb() }
                } else {
                    _error.postValue(EventWrapper(R.string.error_server_unavailable))
                }
            } catch (e: ProtocolException) {
                Log.e("EventViewModel", "Protocol Error in Add: ${e.message}")
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("EventViewModel", "Add Error", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } finally { 
                _isLoading.postValue(false) 
            }
        }
    }

    fun deleteEvent(id: Int, email: String?, cb: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val res = repository.deleteEventFromApi(id, email)
                if (res.isSuccessful && res.body()?.success == true) {
                    fetchEventsFromApi(email, forceRefresh = true)
                    withContext(Dispatchers.Main) { cb() }
                } else {
                    _error.postValue(EventWrapper(R.string.error_server_unavailable))
                }
            } catch (e: ProtocolException) {
                Log.e("EventViewModel", "Protocol Error in Delete: ${e.message}")
                _error.postValue(EventWrapper(R.string.error_network))
            } catch (e: Exception) {
                Log.e("EventViewModel", "Delete Error", e)
                _error.postValue(EventWrapper(R.string.error_network))
            } finally { 
                _isLoading.postValue(false) 
            }
        }
    }
}
