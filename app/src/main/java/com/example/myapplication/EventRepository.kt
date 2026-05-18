package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import retrofit2.Response

class EventRepository(context: Context, private val apiService: ApiService = RetrofitClient.apiService) {
    private val dbHelper = EventDatabaseHelper(context)

    // --- API CALLS ---
    suspend fun getEventsFromApi(): Response<ApiResponse<List<Event>>> {
        return apiService.getEvents()
    }

    suspend fun getEventByIdFromApi(id: Int): Response<ApiResponse<Event>> {
        return apiService.getEventById(id)
    }

    suspend fun addEventToApi(event: Event): Response<ApiResponse<Map<String, Int>>> {
        return apiService.addEvent(event)
    }

    suspend fun updateEventToApi(id: Int, event: Event): Response<ApiResponse<Unit>> {
        return apiService.updateEvent(id, event)
    }

    suspend fun deleteEventFromApi(id: Int): Response<ApiResponse<Unit>> {
        return apiService.deleteEvent(id)
    }

    // --- LOCAL DB CALLS ---
    fun getEventById(id: Int): Event? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )
        var event: Event? = null
        if (cursor.moveToFirst()) {
            event = cursorToEvent(cursor)
        }
        cursor.close()
        db.close()
        return event
    }

    fun setRegistered(id: Int, isRegistered: Boolean, renterEmail: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_IS_REGISTERED, if (isRegistered) 1 else 0)
            put(EventDatabaseHelper.COLUMN_RENTER_EMAIL, renterEmail)
        }
        db.update(EventDatabaseHelper.TABLE_NAME, values, "${EventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
        db.close()
    }

    fun getAllEvents(): List<Event> {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM ${EventDatabaseHelper.TABLE_NAME}", null)
        if (cursor.moveToFirst()) {
            do {
                eventList.add(cursorToEvent(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return eventList
    }

    fun searchVehicles(query: String): List<Event> {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val sql = "SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_NAME} LIKE ?"
        val cursor: Cursor = db.rawQuery(sql, arrayOf("%$query%"))
        if (cursor.moveToFirst()) {
            do {
                eventList.add(cursorToEvent(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return eventList
    }

    fun getEventsByAdmin(adminEmail: String): List<Event> {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_ADMIN_EMAIL} = ?",
            arrayOf(adminEmail)
        )
        if (cursor.moveToFirst()) {
            do {
                eventList.add(cursorToEvent(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return eventList
    }

    fun getRentedUnitsByAdmin(adminEmail: String): List<Event> {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_ADMIN_EMAIL} = ? AND ${EventDatabaseHelper.COLUMN_IS_REGISTERED} = 1",
            arrayOf(adminEmail)
        )
        if (cursor.moveToFirst()) {
            do {
                eventList.add(cursorToEvent(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return eventList
    }

    fun getEventsByRenter(renterEmail: String): List<Event> {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_RENTER_EMAIL} = ?",
            arrayOf(renterEmail)
        )
        if (cursor.moveToFirst()) {
            do {
                eventList.add(cursorToEvent(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return eventList
    }

    private fun cursorToEvent(cursor: Cursor): Event {
        return Event(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_NAME)),
            price = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_PRICE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_DESCRIPTION)),
            // KONVERSI DI SINI: Int ke Boolean
            isRegistered = cursor.getInt(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_IS_REGISTERED)) == 1,
            adminEmail = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_ADMIN_EMAIL)),
            imageUri = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_IMAGE_URI)),
            vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_VEHICLE_TYPE)),
            transmission = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_TRANSMISSION)),
            seats = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_SEATS)),
            location = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_LOCATION)),
            renterEmail = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_RENTER_EMAIL))
        )
    }
}
