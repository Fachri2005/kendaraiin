package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class UserRepository(context: Context) {
    private val dbHelper = EventDatabaseHelper(context)

    fun registerUser(name: String, email: String, password: String, role: String, phone: String = ""): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_USER_NAME, name)
            put(EventDatabaseHelper.COLUMN_USER_EMAIL, email)
            put(EventDatabaseHelper.COLUMN_USER_PASSWORD, password)
            put(EventDatabaseHelper.COLUMN_USER_ROLE, role)
            put(EventDatabaseHelper.COLUMN_USER_PHONE, phone)
            put(EventDatabaseHelper.COLUMN_USER_IMAGE, "")
        }
        val id = db.insert(EventDatabaseHelper.TABLE_USERS, null, values)
        db.close()
        return id
    }

    fun loginUser(email: String, password: String): User? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${EventDatabaseHelper.TABLE_USERS} WHERE ${EventDatabaseHelper.COLUMN_USER_EMAIL} = ? AND ${EventDatabaseHelper.COLUMN_USER_PASSWORD} = ?",
            arrayOf(email, password)
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_ROLE)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_PHONE)) ?: "",
                imageUri = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_IMAGE)) ?: ""
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun getUserByEmail(email: String): User? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${EventDatabaseHelper.TABLE_USERS} WHERE ${EventDatabaseHelper.COLUMN_USER_EMAIL} = ?",
            arrayOf(email)
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_ROLE)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_PHONE)) ?: "",
                imageUri = cursor.getString(cursor.getColumnIndexOrThrow(EventDatabaseHelper.COLUMN_USER_IMAGE)) ?: ""
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun updateUser(email: String, name: String, phone: String, imageUri: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EventDatabaseHelper.COLUMN_USER_NAME, name)
            put(EventDatabaseHelper.COLUMN_USER_PHONE, phone)
            put(EventDatabaseHelper.COLUMN_USER_IMAGE, imageUri)
        }
        val result = db.update(EventDatabaseHelper.TABLE_USERS, values, "${EventDatabaseHelper.COLUMN_USER_EMAIL} = ?", arrayOf(email))
        db.close()
        return result
    }
}

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val phone: String = "",
    val imageUri: String = ""
)