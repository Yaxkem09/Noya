package com.example.noya.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.foundation.layout.*

// Caché de contactos con timestamp para invalidación
internal object ContactsCache {
    var contacts = emptyList<Contact>()
    var lastUpdate: Long = 0
    const val CACHE_DURATION_MS = 60_000L // 60 segundos

    fun isValid(): Boolean {
        return contacts.isNotEmpty() &&
            (System.currentTimeMillis() - lastUpdate) < CACHE_DURATION_MS
    }

    fun update(newContacts: List<Contact>) {
        contacts = newContacts
        lastUpdate = System.currentTimeMillis()
    }

    fun invalidate() {
        contacts = emptyList()
        lastUpdate = 0
    }
}

fun getContacts(context: Context, forceRefresh: Boolean = false): List<Contact> {
    // Retornar caché si es válido y no se fuerza actualización
    if (!forceRefresh && ContactsCache.isValid()) {
        return ContactsCache.contacts
    }

    val contacts = mutableListOf<Contact>()
    val seenNumbers = mutableSetOf<String>() // Para evitar duplicados

    val cursor: Cursor? = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
        val photoUriIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

        while (it.moveToNext()) {
            val id = it.getString(idIndex)
            val name = it.getString(nameIndex) ?: "Sin nombre"
            val number = it.getString(numberIndex) ?: ""
            val photoUri = if (photoUriIndex >= 0) it.getString(photoUriIndex) else null

            // Normalizar el número (quitar espacios, guiones, paréntesis)
            val normalizedNumber = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")

            if (number.isNotEmpty() && !seenNumbers.contains(normalizedNumber)) {
                seenNumbers.add(normalizedNumber)
                contacts.add(Contact(id, name, number, photoUri))
            }
        }
    }

    // Actualizar caché
    ContactsCache.update(contacts)

    return contacts
}

fun getContactInfoFromNumber(context: Context, phoneNumber: String): Pair<String?, String?> {
    try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI
            ),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIndex = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                val name = if (nameIndex >= 0) it.getString(nameIndex) else null
                val photoUri = if (photoIndex >= 0) it.getString(photoIndex) else null
                return Pair(name, photoUri)
            }
        }
    } catch (e: Exception) {
        Log.e("GetContactInfo", "Error: ${e.message}", e)
    }
    return Pair(null, null)
}

// Cache para información de contactos - evita consultas repetidas.
// Se usa desde varios hilos (IO), por eso es ConcurrentHashMap.
// Se limpia cuando cambian los contactos del teléfono (ver NoyaViewModel).
private val contactInfoCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String?, String?>>()

fun clearContactInfoCache() {
    contactInfoCache.clear()
}

fun getContactInfoBatch(context: Context, phoneNumbers: Set<String>): Map<String, Pair<String?, String?>> {
    val result = mutableMapOf<String, Pair<String?, String?>>()
    val numbersToQuery = mutableSetOf<String>()

    // Primero revisar caché
    for (number in phoneNumbers) {
        val cached = contactInfoCache[number]
        if (cached != null) {
            result[number] = cached
        } else {
            numbersToQuery.add(number)
        }
    }

    // Solo consultar los que no están en caché
    if (numbersToQuery.isEmpty()) return result

    try {
        // Consulta batch: obtener todos los contactos del dispositivo una sola vez
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            null
        )

        // Crear mapa de números normalizados a info de contacto
        val contactsMap = mutableMapOf<String, Pair<String?, String?>>()
        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex) ?: continue
                val normalizedNumber = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
                val name = if (nameIndex >= 0) it.getString(nameIndex) else null
                val photoUri = if (photoIndex >= 0) it.getString(photoIndex) else null
                contactsMap[normalizedNumber] = Pair(name, photoUri)
            }
        }

        // Mapear los números solicitados a sus contactos
        for (number in numbersToQuery) {
            val normalizedNumber = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
            val contactInfo = contactsMap[normalizedNumber] ?: Pair(null, null)
            result[number] = contactInfo
            contactInfoCache[number] = contactInfo // Guardar en caché
        }

    } catch (e: Exception) {
        Log.e("GetContactInfoBatch", "Error: ${e.message}", e)
        // Si falla, devolver null para todos
        for (number in numbersToQuery) {
            result[number] = Pair(null, null)
        }
    }

    return result
}

enum class SaveContactResult { SAVED, SAVED_WITHOUT_PHOTO, FAILED }

// Guarda el contacto en el teléfono (no SIM, para poder guardar foto).
// Hace lectura de archivos y base de datos: llamar desde Dispatchers.IO.
fun saveContactWithPhoto(context: Context, name: String, phoneNumber: String, photoUri: Uri?): SaveContactResult {
    val rawContactId: Long
    try {
        // Nombre y número en una sola operación: o se guarda todo o nada
        val operations = arrayListOf(
            android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build(),
            android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build(),
            android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )
        val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        rawContactId = results[0].uri?.lastPathSegment?.toLong() ?: return SaveContactResult.FAILED
    } catch (e: Exception) {
        Log.e("SaveContact", "Error: ${e.message}", e)
        return SaveContactResult.FAILED
    }

    // Invalidar caché de contactos para que se recarguen
    ContactsCache.invalidate()

    if (photoUri == null) return SaveContactResult.SAVED

    // La foto va aparte: si falla, el contacto ya quedó guardado
    return try {
        val photoBytes = loadContactPhotoBytes(context, photoUri) ?: return SaveContactResult.SAVED_WITHOUT_PHOTO
        val photoValues = android.content.ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, photoValues)
            ?: return SaveContactResult.SAVED_WITHOUT_PHOTO
        SaveContactResult.SAVED
    } catch (e: Exception) {
        Log.e("SaveContact", "Error guardando foto: ${e.message}", e)
        SaveContactResult.SAVED_WITHOUT_PHOTO
    }
}

// Lee la foto, la gira según la cámara (EXIF) y la reduce a máximo 720px en JPEG.
// Las fotos de cámara pesan varios MB y el sistema de contactos rechaza fotos tan grandes.
fun loadContactPhotoBytes(context: Context, photoUri: Uri, maxSize: Int = 720): ByteArray? {
    val resolver = context.contentResolver

    // 1. Leer solo las dimensiones
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(photoUri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    // 2. Decodificar en tamaño reducido para no gastar memoria
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= maxSize && bounds.outHeight / (sampleSize * 2) >= maxSize) {
        sampleSize *= 2
    }
    val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = resolver.openInputStream(photoUri)?.use {
        android.graphics.BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: return null

    // 3. Rotación según la orientación guardada por la cámara
    val rotation = try {
        resolver.openInputStream(photoUri)?.use {
            when (android.media.ExifInterface(it).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    } catch (e: Exception) {
        0f
    }

    // 4. Escalar al tamaño final y girar
    val scale = minOf(1f, maxSize.toFloat() / maxOf(decoded.width, decoded.height))
    val matrix = android.graphics.Matrix().apply {
        postScale(scale, scale)
        postRotate(rotation)
    }
    val finalBitmap = android.graphics.Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

    val output = java.io.ByteArrayOutputStream()
    finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, output)
    if (finalBitmap !== decoded) finalBitmap.recycle()
    decoded.recycle()
    return output.toByteArray()
}
