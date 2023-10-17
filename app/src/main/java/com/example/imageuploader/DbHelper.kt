package com.example.imageuploader

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "MyDatabase"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE Contacts (" +
                    "id TEXT," +
                    "name TEXT," +
                    "phoneNumber TEXT" +
                    ");"
        )

        db.execSQL(
            "CREATE TABLE ServiceStatus (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "status TEXT" +
                    ");"
        )
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        TODO("Not yet implemented")
    }

    fun insertContact(contact: ContactItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", contact.name)
            put("phoneNumber", contact.phoneNumber)
            put("id", contact.id)
        }
        return db.insert("Contacts", null, values)
    }

    fun getAllContacts(): ArrayList<ContactItem> {
        val contacts = ArrayList<ContactItem>()
        val query = "SELECT * FROM Contacts"
        val db = readableDatabase
        val cursor = db.rawQuery(query, null)
        try {
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getString(cursor.getColumnIndex("id"))
                    val name = cursor.getString(cursor.getColumnIndex("name"))
                    val phoneNumber = cursor.getString(cursor.getColumnIndex("phoneNumber"))
                    contacts.add(ContactItem(id, name, phoneNumber))
                } while (cursor.moveToNext())
            }
        } catch (e: SQLException) {
            // Handle exceptions
        } finally {
            cursor.close()
        }
        return contacts
    }

    fun updateContact(contact: ContactItem): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", contact.name)
            put("phoneNumber", contact.phoneNumber)
        }
        return db.update(
            "Contacts",
            values,
            "id = ?",
            arrayOf(contact.id.toString())
        )
    }

    fun deleteContact(contactId: String): Int {
        val db = writableDatabase
        return db.delete("Contacts", "id = ?", arrayOf(contactId.toString()))
    }

    fun insertServiceStatus(status: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", status)
        }
        return db.insert("ServiceStatus", null, values)
    }

    fun getServiceStatus(): String {
        val query = "SELECT * FROM ServiceStatus"
        val db = readableDatabase
        val cursor = db.rawQuery(query, null)
        while(cursor.moveToNext()) {
            println("cursor: ${cursor.getString(cursor.getColumnIndex("status"))}")
        }
        return if (cursor.moveToLast()) {
            val status = cursor.getString(cursor.getColumnIndex("status"))
            status
        } else {
            ""
        }
    }

    fun updateServiceStatus(status: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", status)
        }
        return db.update(
            "ServiceStatus",
            values,
            "id = ?",
            arrayOf("0")
        )
    }

}