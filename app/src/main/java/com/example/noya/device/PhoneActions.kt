package com.example.noya.device

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import com.example.noya.data.BatteryStatus
import java.text.SimpleDateFormat
import java.util.*

fun makePhoneCall(context: Context, phoneNumber: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val uri = Uri.fromParts("tel", phoneNumber, null)

            // Verificar que somos la app de marcación predeterminada
            if (telecomManager.defaultDialerPackage == context.packageName) {
                // Usar TelecomManager para realizar la llamada
                val bundle = Bundle()
                telecomManager.placeCall(uri, bundle)
            } else {
                // Si no somos la app predeterminada, solicitar que nos configuren
                Toast.makeText(context, "Por favor, configura Noya como app de teléfono predeterminada", Toast.LENGTH_LONG).show()
            }
        } else {
            // Para versiones anteriores a Android 6.0
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "No se puede realizar la llamada. Verifica los permisos.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al realizar la llamada: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun toggleSilentMode(context: Context, onModeChanged: (Boolean) -> Unit) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // Solicitar permiso para modificar el modo No Molestar
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            context.startActivity(intent)
            Toast.makeText(context, "Por favor, concede permiso para controlar el sonido", Toast.LENGTH_LONG).show()
            return
        }
    }

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            onModeChanged(true)
            Toast.makeText(context, "Modo silencio activado. El sonido volverá en 2 horas", Toast.LENGTH_LONG).show()
        }
        else -> {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            onModeChanged(false)
            Toast.makeText(context, "Sonido activado", Toast.LENGTH_SHORT).show()
        }
    }
}

fun getBatteryStatus(context: Context): BatteryStatus {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                     status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryStatus(level, isCharging)
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
    return sdf.format(Date())
}
