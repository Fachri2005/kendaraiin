package com.example.myapplication

/**
 * Digunakan sebagai pembungkus data yang dikirim melalui LiveData untuk merepresentasikan sebuah event.
 * Ini mencegah event yang sama diproses lebih dari satu kali (misalnya saat rotasi layar).
 */
open class EventWrapper<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    /**
     * Mengembalikan konten jika belum pernah ditangani, dan menandainya sebagai sudah ditangani.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Mengembalikan konten, meskipun sudah pernah ditangani.
     */
    fun peekContent(): T = content
}
