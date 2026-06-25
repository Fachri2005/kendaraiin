package com.example.myapplication

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EventViewModel::class.java) -> {
                val eventRepository = EventRepository(context)
                val userRepository = UserRepository(context)
                @Suppress("UNCHECKED_CAST")
                EventViewModel(eventRepository, userRepository, context.applicationContext) as T
            }
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                val repository = UserRepository(context)
                @Suppress("UNCHECKED_CAST")
                UserViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
