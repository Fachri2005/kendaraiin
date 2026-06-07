package com.example.myapplication

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Event(
    val id: Int = 0,
    val name: String,
    val price: String,
    val description: String,
    @SerializedName("is_registered")
    val isRegistered: Boolean = false,
    @SerializedName("admin_email")
    val adminEmail: String? = null,
    @SerializedName("image_uri")
    val imageUri: String? = null,
    @SerializedName("vehicle_type")
    val vehicleType: String? = null,
    val transmission: String? = "Matic",
    val seats: String? = "5 Kursi",
    val location: String? = "Jakarta",
    @SerializedName("renter_email")
    val renterEmail: String? = null,
    
    // Tambahan untuk Form Sewa
    @SerializedName("rental_start_date")
    val rentalStartDate: String? = null,
    @SerializedName("rental_duration")
    val rentalDuration: Int? = null,
    @SerializedName("pickup_location")
    val pickupLocation: String? = null
) {
    /**
     * Cek apakah kendaraan benar-benar masih dalam masa sewa.
     * Jika sudah lewat tanggal berakhir, dianggap TIDAK terdaftar (Tersedia).
     */
    val isAvailable: Boolean
        get() {
            // Jika memang dari awal tidak disewa
            if (!isRegistered) return true
            
            // Jika data sewa tidak lengkap, anggap tersedia (safety check)
            if (rentalStartDate.isNullOrEmpty() || rentalDuration == null) return true

            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDate = sdf.parse(rentalStartDate) ?: return true
                
                val calendar = Calendar.getInstance()
                calendar.time = startDate
                calendar.add(Calendar.DAY_OF_YEAR, rentalDuration)
                val endDate = calendar.time
                
                // Jika waktu sekarang sudah melewati (after) end date, maka tersedia (true)
                val now = Date()
                now.after(endDate) 
            } catch (e: Exception) {
                true // Jika error parsing, anggap tersedia
            }
        }
    
    /**
     * Mendapatkan status isRegistered yang "cerdas" (memperhitungkan waktu).
     */
    fun getEffectiveIsRegistered(): Boolean {
        // Jika isRegistered true tapi waktu sudah habis, kembalikan false
        if (isRegistered && isAvailable) return false
        return isRegistered
    }
}
