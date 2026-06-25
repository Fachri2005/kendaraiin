package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class EventRepository(context: Context, val apiService: ApiService = RetrofitClient.apiService) {
    private val appContext = context.applicationContext
    private val dbHelper = EventDatabaseHelper(appContext)

    suspend fun getEventsFromApi(adminEmail: String? = null): Response<ApiResponse<List<Event>>> = apiService.getEvents(adminEmail)
    suspend fun getEventByIdFromApi(id: Int): Response<ApiResponse<Event>> = apiService.getEventById(id)
    suspend fun addEventToApi(event: Event): Response<ApiResponse<Map<String, Int>>> = apiService.addEvent(event)
    suspend fun updateEventToApi(id: Int, event: Event): Response<ApiResponse<Unit>> = apiService.updateEvent(id, event)
    suspend fun deleteEventFromApi(id: Int, adminEmail: String? = null): Response<ApiResponse<Unit>> = apiService.deleteEvent(id, adminEmail)

    suspend fun saveEventsToLocal(events: List<Event>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (event in events) {
                val values = ContentValues().apply {
                    put(EventDatabaseHelper.COLUMN_ID, event.id)
                    put(EventDatabaseHelper.COLUMN_NAME, event.name)
                    put(EventDatabaseHelper.COLUMN_PRICE, event.price)
                    put(EventDatabaseHelper.COLUMN_DESCRIPTION, event.description)
                    put(EventDatabaseHelper.COLUMN_IS_REGISTERED, if (event.isRegistered) 1 else 0)
                    put(EventDatabaseHelper.COLUMN_ADMIN_EMAIL, event.adminEmail)
                    put(EventDatabaseHelper.COLUMN_IMAGE_URI, event.imageUri)
                    put(EventDatabaseHelper.COLUMN_VEHICLE_TYPE, event.vehicleType)
                    put(EventDatabaseHelper.COLUMN_TRANSMISSION, event.transmission)
                    put(EventDatabaseHelper.COLUMN_SEATS, event.seats)
                    put(EventDatabaseHelper.COLUMN_LOCATION, event.location)
                    put(EventDatabaseHelper.COLUMN_RENTER_EMAIL, event.renterEmail)
                    put(EventDatabaseHelper.COLUMN_RENTAL_START_DATE, event.rentalStartDate)
                    put(EventDatabaseHelper.COLUMN_RENTAL_DURATION, event.rentalDuration)
                    put(EventDatabaseHelper.COLUMN_PICKUP_LOCATION, event.pickupLocation)
                    put(EventDatabaseHelper.COLUMN_RENTAL_STATUS, event.rentalStatus)
                }
                db.insertWithOnConflict(EventDatabaseHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun updateRentalStatusLocal(id: Int, status: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_RENTAL_STATUS, status)
            if (status == "approved") put(EventDatabaseHelper.COLUMN_IS_REGISTERED, 1)
            else if (status == "canceled" || status == "completed") put(EventDatabaseHelper.COLUMN_IS_REGISTERED, 0)
        }
        val result = db.update(EventDatabaseHelper.TABLE_NAME, values, "${EventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
        result > 0
    }

    suspend fun getAllEventsFromLocal(): List<Event> = withContext(Dispatchers.IO) {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} ORDER BY ${EventDatabaseHelper.COLUMN_ID} DESC", null)
        if (cursor.moveToFirst()) {
            do { eventList.add(cursorToEvent(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        eventList
    }

    suspend fun getEventById(id: Int): Event? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
        var event: Event? = null
        if (cursor.moveToFirst()) event = cursorToEvent(cursor)
        cursor.close()
        event
    }

    suspend fun getEventsByAdmin(adminEmail: String): List<Event> = withContext(Dispatchers.IO) {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_ADMIN_EMAIL} = ?",
            arrayOf(adminEmail)
        )
        if (cursor.moveToFirst()) {
            do { eventList.add(cursorToEvent(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        eventList
    }

    suspend fun rentVehicleLocal(id: Int, renterEmail: String, startDate: String, duration: Int, pickup: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_IS_REGISTERED, 1)
            put(EventDatabaseHelper.COLUMN_RENTER_EMAIL, renterEmail)
            put(EventDatabaseHelper.COLUMN_RENTAL_START_DATE, startDate)
            put(EventDatabaseHelper.COLUMN_RENTAL_DURATION, duration)
            put(EventDatabaseHelper.COLUMN_PICKUP_LOCATION, pickup)
            put(EventDatabaseHelper.COLUMN_RENTAL_STATUS, "pending")
        }
        val result = db.update(EventDatabaseHelper.TABLE_NAME, values, "${EventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
        result > 0
    }

    suspend fun searchVehicles(query: String): List<Event> = withContext(Dispatchers.IO) {
        val eventList = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM ${EventDatabaseHelper.TABLE_NAME} WHERE ${EventDatabaseHelper.COLUMN_NAME} LIKE ?", arrayOf("%$query%"))
        if (cursor.moveToFirst()) {
            do { eventList.add(cursorToEvent(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        eventList
    }

    private fun cursorToEvent(cursor: Cursor): Event {
        return Event(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_NAME)),
            price = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_PRICE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_DESCRIPTION)),
            isRegistered = cursor.getInt(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_IS_REGISTERED)) == 1,
            adminEmail = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_ADMIN_EMAIL)),
            imageUri = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_IMAGE_URI)),
            vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_VEHICLE_TYPE)),
            transmission = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_TRANSMISSION)),
            seats = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_SEATS)),
            location = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_LOCATION)),
            renterEmail = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_RENTER_EMAIL)),
            rentalStartDate = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_RENTAL_START_DATE)),
            rentalDuration = cursor.getInt(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_RENTAL_DURATION)),
            pickupLocation = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_PICKUP_LOCATION)),
            rentalStatus = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_RENTAL_STATUS))
        )
    }
}
