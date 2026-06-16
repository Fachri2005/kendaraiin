package com.example.myapplication

import com.google.gson.annotations.SerializedName

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
     * Mendapatkan status yang sudah diolah secara pintar.
     * Mengutamakan data rentalStatus, lalu fallback ke isRegistered.
     */
    val effectiveStatus: String
        get() {
            val s = rentalStatus?.toString()?.lowercase()?.trim() ?: ""
            
            // Jika tidak ada email penyewa, maka kendaraan harusnya berstatus tersedia (kosong)
            if (renterEmail.isNullOrEmpty()) return ""

            return when (s) {
                "approved", "1", "setuju", "sukses", "berhasil", "active" -> "approved"
                "canceled", "2", "rejected", "tolak", "batal" -> "canceled"
                "completed", "3", "finished", "selesai" -> "completed"
                "pending", "0", "menunggu", "wait", "" -> "pending"
                else -> s
            }
        }

    val isAvailable: Boolean
        get() {
            val status = effectiveStatus
            return status != "pending" && status != "approved"
        }
}
