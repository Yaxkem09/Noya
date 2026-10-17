package com.example.noya

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat

// Notificación de llamada entrante. Es la red de seguridad por si el teléfono
// no deja abrir la pantalla de llamada directamente: Android usa el
// fullScreenIntent para abrirla (sobre todo con el teléfono bloqueado) y, si no,
// muestra un aviso con botones Contestar / Rechazar.
object IncomingCallNotification {
    private const val TAG = "IncomingCallNotif"
    private const val CHANNEL_ID = "incoming_calls"
    private const val NOTIFICATION_ID = 1001

    const val EXTRA_ANSWER_CALL = "ANSWER_CALL"

    fun show(context: Context, callerNumber: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Sin permiso de notificaciones - no se muestra la notificación de llamada")
            return
        }

        createChannel(context)

        val callerName = getContactInfoFromNumber(context, callerNumber).first
        val caller = Person.Builder()
            .setName(callerName ?: callerNumber)
            .setImportant(true)
            .build()

        // Abrir la pantalla de llamada entrante
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            0,
            callScreenIntent(context, callerNumber, answer = false),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Contestar: abre Noya y contesta, para que se vea el botón "Colgar"
        val answerIntent = PendingIntent.getActivity(
            context,
            1,
            callScreenIntent(context, callerNumber, answer = true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Rechazar: no hace falta abrir la app
        val declineIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, IncomingCallActionReceiver::class.java).apply {
                action = IncomingCallActionReceiver.ACTION_DECLINE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Llamada entrante")
            .setContentText(callerName ?: callerNumber)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declineIntent, answerIntent))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notificación de llamada entrante mostrada")
        } catch (e: SecurityException) {
            Log.e(TAG, "Error mostrando notificación: ${e.message}", e)
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun callScreenIntent(context: Context, callerNumber: String, answer: Boolean): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("INCOMING_CALL", true)
            putExtra("CALLER_NUMBER", callerNumber)
            putExtra(EXTRA_ANSWER_CALL, answer)
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Llamadas entrantes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Muestra la pantalla para contestar llamadas"
            // El timbre y la vibración ya los pone el sistema de llamadas
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

class IncomingCallActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DECLINE = "com.example.noya.ACTION_DECLINE_CALL"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_DECLINE) {
            Log.d("IncomingCallAction", "Rechazar desde la notificación")
            CallManager.rejectCall()
            IncomingCallNotification.cancel(context)
        }
    }
}
