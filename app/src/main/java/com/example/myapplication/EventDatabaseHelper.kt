package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class EventDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kendaraiin.db"
        private const val DATABASE_VERSION = 11 // Upgraded to include rental_status
        
        const val TABLE_NAME = "events"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_PRICE = "price"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_IS_REGISTERED = "is_registered"
        const val COLUMN_ADMIN_EMAIL = "admin_email"
        const val COLUMN_IMAGE_URI = "image_uri"
        const val COLUMN_VEHICLE_TYPE = "vehicle_type"
        const val COLUMN_TRANSMISSION = "transmission"
        const val COLUMN_SEATS = "seats"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_RENTER_EMAIL = "renter_email"
        
        // Rental columns
        const val COLUMN_RENTAL_START_DATE = "rental_start_date"
        const val COLUMN_RENTAL_DURATION = "rental_duration"
        const val COLUMN_PICKUP_LOCATION = "pickup_location"
        const val COLUMN_RENTAL_STATUS = "rental_status" // New column

        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_USER_NAME = "user_name"
        const val COLUMN_USER_EMAIL = "user_email"
        const val COLUMN_USER_PASSWORD = "user_password"
        const val COLUMN_USER_ROLE = "user_role"
        const val COLUMN_USER_PHONE = "user_phone"
        const val COLUMN_USER_IMAGE = "user_image"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createEventsTable = ("CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_NAME TEXT," +
                "$COLUMN_PRICE TEXT," +
                "$COLUMN_DESCRIPTION TEXT," +
                "$COLUMN_IS_REGISTERED INTEGER DEFAULT 0," +
                "$COLUMN_ADMIN_EMAIL TEXT," +
                "$COLUMN_IMAGE_URI TEXT," +
                "$COLUMN_VEHICLE_TYPE TEXT," +
                "$COLUMN_TRANSMISSION TEXT," +
                "$COLUMN_SEATS TEXT," +
                "$COLUMN_LOCATION TEXT," +
                "$COLUMN_RENTER_EMAIL TEXT," +
                "$COLUMN_RENTAL_START_DATE TEXT," +
                "$COLUMN_RENTAL_DURATION INTEGER," +
                "$COLUMN_PICKUP_LOCATION TEXT," +
                "$COLUMN_RENTAL_STATUS TEXT)")
        db.execSQL(createEventsTable)

        val createUsersTable = ("CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_USER_NAME TEXT," +
                "$COLUMN_USER_EMAIL TEXT UNIQUE," +
                "$COLUMN_USER_PASSWORD TEXT," +
                "$COLUMN_USER_ROLE TEXT," +
                "$COLUMN_USER_PHONE TEXT," +
                "$COLUMN_USER_IMAGE TEXT)")
        db.execSQL(createUsersTable)

        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_RENTAL_START_DATE TEXT")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_RENTAL_DURATION INTEGER")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_PICKUP_LOCATION TEXT")
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_RENTAL_STATUS TEXT")
        }
    }

    private fun insertSampleData(db: SQLiteDatabase) {
        val samples = arrayOf(
            arrayOf("Toyota Avanza 2022", "Rp 450.000", "Mobil keluarga nyaman.", "Mobil", "Matic", "7 Kursi", "Jakarta Selatan"),
            arrayOf("Honda CR-V 2021", "Rp 800.000", "SUV premium bertenaga.", "Mobil", "Matic", "5 Kursi", "Tangerang"),
            arrayOf("Honda Vario 160", "Rp 150.000", "Motor harian irit.", "Motor", "Otomatis", "2 Kursi", "Jakarta Pusat"),
            arrayOf("Yamaha NMAX 155", "Rp 175.000", "Motor matic gambot.", "Motor", "Otomatis", "2 Kursi", "Bekasi")
        )

        for (sample in samples) {
            val values = ContentValues().apply {
                put(COLUMN_NAME, sample[0])
                put(COLUMN_PRICE, sample[1])
                put(COLUMN_DESCRIPTION, sample[2])
                put(COLUMN_IS_REGISTERED, 0)
                put(COLUMN_ADMIN_EMAIL, "admin@gmail.com")
                put(COLUMN_IMAGE_URI, "")
                put(COLUMN_VEHICLE_TYPE, sample[3])
                put(COLUMN_TRANSMISSION, sample[4])
                put(COLUMN_SEATS, sample[5])
                put(COLUMN_LOCATION, sample[6])
                put(COLUMN_RENTER_EMAIL, "")
            }
            db.insert(TABLE_NAME, null, values)
        }
    }
}
