package com.example.myapplication

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

private val ID_LOCALE = Locale("in", "ID")
private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val DIGIT_ONLY = Regex("[^0-9]")

data class Event(
    val id: Int = 0,
    val name: String? = null,
    val price: String? = null,
    val description: String? = null,
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
    
    @SerializedName(value = "rental_start_date", alternate = ["start_date"])
    val rentalStartDate: String? = null,
    @SerializedName(value = "rental_duration", alternate = ["duration"])
    val rentalDuration: Int? = null,
    @SerializedName("pickup_location")
    val pickupLocation: String? = null,

    @SerializedName(value = "rental_status", alternate = ["status", "rentalStatus", "status_sewa"])
    val rentalStatus: String? = null
) {
    // Sederhanakan caching tanpa synchronized untuk startup yang lebih stabil
    @Transient private var _cachedStatus: String? = null
    @Transient private var _cachedPrice: Long? = null

    val effectiveStatus: String
        get() {
            if (_cachedStatus != null) return _cachedStatus!!
            _cachedStatus = calculateStatus()
            return _cachedStatus!!
        }

    private fun calculateStatus(): String {
        val s = rentalStatus?.lowercase()?.trim() ?: ""
        if (renterEmail.isNullOrEmpty()) return ""

        val baseStatus = when (s) {
            "approved", "1", "setuju", "sukses", "berhasil", "active" -> "approved"
            "canceled", "2", "rejected", "tolak", "batal" -> "canceled"
            "completed", "3", "finished", "selesai" -> "completed"
            "pending", "0", "menunggu", "wait", "" -> "pending"
            else -> s
        }

        if (baseStatus == "approved" && !rentalStartDate.isNullOrEmpty() && rentalDuration != null) {
            try {
                // Gunakan blok synchronized hanya pada objek formatnya saja
                val startDate = synchronized(ISO_FORMAT) { ISO_FORMAT.parse(rentalStartDate) }
                if (startDate != null) {
                    val calendar = Calendar.getInstance().apply {
                        time = startDate
                        add(Calendar.DAY_OF_YEAR, rentalDuration)
                    }
                    if (Date().after(calendar.time)) return "completed"
                }
            } catch (e: Exception) { }
        }
        return baseStatus
    }

    val numericPrice: Long
        get() {
            if (_cachedPrice != null) return _cachedPrice!!
            _cachedPrice = price?.replace(DIGIT_ONLY, "")?.toLongOrNull() ?: 0L
            return _cachedPrice!!
        }

    val formattedPrice: String
        get() = try {
            String.format(ID_LOCALE, "%,d", numericPrice).replace(',', '.')
        } catch (e: Exception) { "0" }

    val isAvailable: Boolean
        get() = effectiveStatus.let { it != "pending" && it != "approved" }
}
