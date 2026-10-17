package com.example.noya

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import org.robolectric.Robolectric

private fun normalize(number: String) = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")

// Agenda de contactos simulada (authority com.android.contacts)
class FakeContactsProvider : ContentProvider() {

    data class Row(val id: String, val name: String?, val number: String?, val photoUri: String? = null)

    val rows = mutableListOf<Row>()

    // Lo que se insertó (para probar "Nuevo contacto")
    val insertedRawContacts = mutableListOf<ContentValues>()
    val insertedData = mutableListOf<ContentValues>()

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val columns = projection ?: arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        val cursor = MatrixCursor(columns)

        val matching = when (uri.pathSegments.firstOrNull()) {
            // PhoneLookup: content://com.android.contacts/phone_lookup/<número>
            "phone_lookup" -> {
                val wanted = normalize(uri.lastPathSegment.orEmpty())
                rows.filter { it.number != null && normalize(it.number) == wanted }
            }
            else -> {
                if (sortOrder?.contains(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) == true) {
                    rows.sortedBy { it.name ?: "" }
                } else {
                    rows
                }
            }
        }

        for (row in matching) {
            cursor.addRow(columns.map { column ->
                when (column) {
                    ContactsContract.CommonDataKinds.Phone._ID -> row.id
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.DISPLAY_NAME -> row.name
                    ContactsContract.CommonDataKinds.Phone.NUMBER -> row.number
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                    ContactsContract.PhoneLookup.PHOTO_URI -> row.photoUri
                    else -> null
                }
            })
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val copy = ContentValues(values ?: ContentValues())
        return if (uri.pathSegments.firstOrNull() == "raw_contacts") {
            insertedRawContacts.add(copy)
            Uri.withAppendedPath(ContactsContract.RawContacts.CONTENT_URI, insertedRawContacts.size.toString())
        } else {
            insertedData.add(copy)
            Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, insertedData.size.toString())
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        fun install(): FakeContactsProvider =
            Robolectric.setupContentProvider(FakeContactsProvider::class.java, ContactsContract.AUTHORITY)
    }
}

// Historial de llamadas simulado (authority call_log)
class FakeCallLogProvider : ContentProvider() {

    data class Row(val id: String, val number: String, val date: Long, val type: Int)

    val rows = mutableListOf<Row>()

    override fun onCreate() = true

    // Entiende la consulta que hace la app: "type = ? AND date >= ?"
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val columns = projection ?: arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.DATE)
        val type = selectionArgs?.getOrNull(0)?.toInt()
        val minDate = selectionArgs?.getOrNull(1)?.toLong() ?: 0L

        var matching = rows.filter { (type == null || it.type == type) && it.date >= minDate }
        if (sortOrder?.contains("DESC") == true) {
            matching = matching.sortedByDescending { it.date }
        }

        val cursor = MatrixCursor(columns)
        for (row in matching) {
            cursor.addRow(columns.map { column ->
                when (column) {
                    CallLog.Calls._ID -> row.id
                    CallLog.Calls.NUMBER -> row.number
                    CallLog.Calls.DATE -> row.date
                    else -> null
                }
            })
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun getType(uri: Uri): String? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        fun install(): FakeCallLogProvider =
            Robolectric.setupContentProvider(FakeCallLogProvider::class.java, CallLog.AUTHORITY)
    }
}
