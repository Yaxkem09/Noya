package com.example.noya

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log

// Devuelve el sonido automáticamente después de un tiempo en silencio,
// para que no se pierdan llamadas si se olvida quitar el silencio.
object SilentModeTimer {
    private const val TAG = "SilentModeTimer"
    private const val PREFS_NAME = "noya_settings"
    private const val KEY_RESTORE_AT = "silent_restore_at"
    const val SILENT_DURATION_MS = 2 * 60 * 60 * 1000L // 2 horas

    // Programar la alarma si no hay una pendiente.
    // Se llama cada vez que se detecta el teléfono en silencio o vibración.
    // La hora se guarda para no reiniciar el conteo y para reprogramar
    // la alarma si se perdió (por ejemplo, después de reiniciar el teléfono).
    fun ensureScheduled(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        var restoreAt = prefs.getLong(KEY_RESTORE_AT, 0L)

        if (restoreAt <= now) {
            restoreAt = now + SILENT_DURATION_MS
            prefs.edit().putLong(KEY_RESTORE_AT, restoreAt).apply()
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = restorePendingIntent(context)

        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restoreAt, pendingIntent)
            } else {
                // Sin permiso de alarma exacta: ventana de 10 minutos
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, restoreAt, 10 * 60 * 1000L, pendingIntent)
            }
            Log.d(TAG, "Sonido se restaurará en ${(restoreAt - now) / 60000} minutos (exacta: $canExact)")
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar restauración de sonido: ${e.message}", e)
        }
    }

    // Cancelar la alarma cuando el teléfono vuelve a tener sonido
    fun cancel(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_RESTORE_AT)) return

        prefs.edit().remove(KEY_RESTORE_AT).apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(restorePendingIntent(context))
        Log.d(TAG, "Restauración de sonido cancelada")
    }

    fun restoreSound(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_RESTORE_AT).apply()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            Log.e(TAG, "Sin permiso de No Molestar - no se puede restaurar el sonido")
            return
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            Log.d(TAG, "Sonido restaurado automáticamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al restaurar sonido: ${e.message}", e)
        }
    }

    private fun restorePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SilentModeRestoreReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class SilentModeRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("SilentModeRestoreReceiver", "Alarma recibida - restaurando sonido")
        SilentModeTimer.restoreSound(context)
    }
}
