package com.example.noya.data

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

fun getMissedCalls(context: Context): List<CallLog> {
    val missedCalls = mutableListOf<CallLog>()

    try {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        // Datos temporales de llamadas perdidas (sin info de contacto aún)
        data class TempCallData(val id: String, val number: String, val dateMillis: Long)
        val tempMissedCalls = mutableListOf<TempCallData>()

        // 1. Obtener llamadas perdidas (solo datos básicos)
        context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            arrayOf(
                android.provider.CallLog.Calls._ID,
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.DATE
            ),
            "${android.provider.CallLog.Calls.TYPE} = ? AND ${android.provider.CallLog.Calls.DATE} >= ?",
            arrayOf(android.provider.CallLog.Calls.MISSED_TYPE.toString(), twentyFourHoursAgo.toString()),
            "${android.provider.CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(android.provider.CallLog.Calls._ID)
            val numberIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val dateIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE)

            while (cursor.moveToNext()) {
                tempMissedCalls.add(TempCallData(
                    id = cursor.getString(idIndex),
                    number = cursor.getString(numberIndex) ?: "Desconocido",
                    dateMillis = cursor.getLong(dateIndex)
                ))
            }
        }

        // 2. Obtener llamadas salientes para filtrar las devueltas
        val outgoingCalls = mutableListOf<Pair<String, Long>>()
        context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            arrayOf(
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.DATE
            ),
            "${android.provider.CallLog.Calls.TYPE} = ? AND ${android.provider.CallLog.Calls.DATE} >= ?",
            arrayOf(android.provider.CallLog.Calls.OUTGOING_TYPE.toString(), twentyFourHoursAgo.toString()),
            null
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val dateIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex) ?: ""
                if (number.isNotEmpty()) {
                    outgoingCalls.add(Pair(
                        number.replace(Regex("[\\s\\-\\(\\)\\.]"), ""),
                        cursor.getLong(dateIndex)
                    ))
                }
            }
        }

        // 3. Filtrar llamadas no devueltas
        val unreturned = tempMissedCalls.filter { missed ->
            val normalizedMissed = missed.number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
            !outgoingCalls.any { (outNum, outDate) ->
                outNum == normalizedMissed && outDate > missed.dateMillis
            }
        }

        // 4. Filtrar duplicados - solo mostrar una llamada por número (la más reciente)
        val unreturnedUnique = unreturned
            .sortedByDescending { it.dateMillis }
            .distinctBy { it.number.replace(Regex("[\\s\\-\\(\\)\\.]"), "") }

        // 5. Obtener info de contactos en BATCH (una sola consulta)
        val uniqueNumbers = unreturnedUnique.map { it.number }.toSet()
        val contactsInfo = getContactInfoBatch(context, uniqueNumbers)

        // 6. Construir la lista final
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (call in unreturnedUnique) {
            val (name, photoUri) = contactsInfo[call.number] ?: Pair(null, null)
            missedCalls.add(CallLog(
                id = call.id,
                phoneNumber = call.number,
                name = name,
                date = dateFormat.format(Date(call.dateMillis)),
                time = timeFormat.format(Date(call.dateMillis)),
                photoUri = photoUri
            ))
        }

    } catch (e: Exception) {
        Log.e("MissedCalls", "Error obteniendo llamadas perdidas: ${e.message}", e)
    }

    return missedCalls
}
