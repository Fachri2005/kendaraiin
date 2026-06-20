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
    
    @SerializedName("rental_start_date")
    val rentalStartDate: String? = null,
    @SerializedName("rental_duration")
    val rentalDuration: Int? = null,
    @SerializedName("pickup_location")
    val pickupLocation: String? = null,

    @SerializedName(value = "rental_status", alternate = ["status", "rentalStatus", "status_sewa"])
    val rentalStatus: String? = null
) {
    /**
     * Mendapatkan harga dalam bentuk angka murni (Long)
     */
    val numericPrice: Long
        get() {
            return try {
                val cleanString = price.replace(Regex("[^0-9]"), "")
                cleanString.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

    /**
     * Mendapatkan angka harga yang sudah diformat titik (Contoh: 1.000.000)
     * Teks "Rp" akan diambil dari strings.xml agar tidak dobel.
     */
    val formattedPrice: String
        get() = String.format(Locale("in", "ID"), "%,d", numericPrice).replace(',', '.')

    /**
     * Mendapatkan status yang sudah diolah secara pintar.
     */
    val effectiveStatus: String
        get() {
            val s = rentalStatus?.toString()?.lowercase()?.trim() ?: ""
            if (renterEmail.isNullOrEmpty()) return ""

            val baseStatus = when (s) {
                "approved", "1", "setuju", "sukses", "berhasil", "active" -> "approved"
                "canceled", "2", "rejected", "tolak", "batal" -> "canceled"
                "completed", "3", "finished", "selesai" -> "completed"
                "pending", "0", "menunggu", "wait", "" -> "pending"
                else -> s
            }

            // Logika Auto-Finished: Jika approved tapi sudah melewati masa sewa
            if (baseStatus == "approved" && !rentalStartDate.isNullOrEmpty() && rentalDuration != null) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startDate = sdf.parse(rentalStartDate)
                    if (startDate != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = startDate
                        calendar.add(Calendar.DAY_OF_YEAR, rentalDuration)
                        
                        val endDate = calendar.time
                        val today = Date()

                        if (today.after(endDate)) {
                            return "completed"
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse error
                }
            }

            return baseStatus
        }

    val isAvailable: Boolean
        get() {
            val status = effectiveStatus
            return status != "pending" && status != "approved"
        }
}
