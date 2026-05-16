package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class EventRepository(context: Context) {
    private val dbHelper = EventDatabaseHelper(context)

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
        
        val sql = """
            SELECT e.* FROM ${EventDatabaseHelper.TABLE_NAME} e
            LEFT JOIN ${EventDatabaseHelper.TABLE_USERS} u ON e.${EventDatabaseHelper.COLUMN_ADMIN_EMAIL} = u.${EventDatabaseHelper.COLUMN_USER_EMAIL}
            WHERE e.${EventDatabaseHelper.COLUMN_NAME} LIKE ? 
            OR u.${EventDatabaseHelper.COLUMN_USER_NAME} LIKE ?
        """.trimIndent()
        
        val searchPattern = "%$query%"
        val cursor: Cursor = db.rawQuery(sql, arrayOf(searchPattern, searchPattern))

        if (cursor.moveToFirst()) {
            do {
                eventList.add(cursorToEvent(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return eventList
    }

    fun getEventById(id: Int): Event? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
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

    fun addEvent(name: String, price: String, description: String, adminEmail: String, imageUri: String?, vehicleType: String, transmission: String, seats: String, location: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_NAME, name)
            put(EventDatabaseHelper.COLUMN_PRICE, price)
            put(EventDatabaseHelper.COLUMN_DESCRIPTION, description)
            put(EventDatabaseHelper.COLUMN_IS_REGISTERED, 0)
            put(EventDatabaseHelper.COLUMN_ADMIN_EMAIL, adminEmail)
            put(EventDatabaseHelper.COLUMN_IMAGE_URI, imageUri)
            put(EventDatabaseHelper.COLUMN_VEHICLE_TYPE, vehicleType)
            put(EventDatabaseHelper.COLUMN_TRANSMISSION, transmission)
            put(EventDatabaseHelper.COLUMN_SEATS, seats)
            put(EventDatabaseHelper.COLUMN_LOCATION, location)
            put(EventDatabaseHelper.COLUMN_RENTER_EMAIL, "")
        }
        val id = db.insert(EventDatabaseHelper.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun updateEvent(id: Int, name: String, price: String, description: String, imageUri: String?, vehicleType: String, transmission: String, seats: String, location: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_NAME, name)
            put(EventDatabaseHelper.COLUMN_PRICE, price)
            put(EventDatabaseHelper.COLUMN_DESCRIPTION, description)
            put(EventDatabaseHelper.COLUMN_VEHICLE_TYPE, vehicleType)
            put(EventDatabaseHelper.COLUMN_TRANSMISSION, transmission)
            put(EventDatabaseHelper.COLUMN_SEATS, seats)
            put(EventDatabaseHelper.COLUMN_LOCATION, location)
            if (imageUri != null) {
                put(EventDatabaseHelper.COLUMN_IMAGE_URI, imageUri)
            }
        }
        val result = db.update(EventDatabaseHelper.TABLE_NAME, values, "${EventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
        db.close()
        return result
    }

    fun setRegistered(id: Int, isRegistered: Boolean, renterEmail: String? = ""): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_IS_REGISTERED, if (isRegistered) 1 else 0)
            put(EventDatabaseHelper.COLUMN_RENTER_EMAIL, renterEmail)
        }
        val result = db.update(EventDatabaseHelper.TABLE_NAME, values, "${EventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
        db.close()
        return result
    }

    fun deleteEvent(id: Int): Int {
        val db = dbHelper.writableDatabase
        val result = db.delete(EventDatabaseHelper.TABLE_NAME, "${EventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
        db.close()
        return result
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
            renterEmail = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_RENTER_EMAIL))
        )
    }
}