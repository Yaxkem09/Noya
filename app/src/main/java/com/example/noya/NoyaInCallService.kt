package com.example.noya

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class NoyaInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        Log.d("NoyaInCallService", "Llamada añadida: ${call.details.handle}")

        // Notificar que hay una llamada activa
        CallManager.ongoingCall = call
        CallManager.setCallState(call.state)

        // Si es una llamada entrante (ringing), mostrar la UI
        if (call.state == Call.STATE_RINGING) {
            Log.d("NoyaInCallService", "Llamada entrante detectada")

            // Lanzar la actividad principal para mostrar la llamada entrante
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                putExtra("INCOMING_CALL", true)
                putExtra("CALLER_NUMBER", call.details.handle?.schemeSpecificPart ?: "Desconocido")
            }
            startActivity(intent)
        }

        // Agregar listener para cambios de estado
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                Log.d("NoyaInCallService", "Estado de llamada cambiado: $state")
                CallManager.setCallState(state)

                if (state == Call.STATE_DISCONNECTED) {
                    CallManager.ongoingCall = null
                }
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("NoyaInCallService", "Llamada removida")
        CallManager.ongoingCall = null
        CallManager.setCallState(Call.STATE_DISCONNECTED)
    }
}

object CallManager {
    var ongoingCall: Call? = null
    private var callStateCallback: ((Int) -> Unit)? = null

    fun setCallState(state: Int) {
        callStateCallback?.invoke(state)
    }

    fun setCallStateCallback(callback: (Int) -> Unit) {
        callStateCallback = callback
    }

    fun endCall() {
        ongoingCall?.disconnect()
    }

    fun answerCall() {
        ongoingCall?.answer(0) // 0 = video state (audio only)
    }

    fun rejectCall() {
        ongoingCall?.reject(false, null)
    }

    fun setAudioRoute(route: Int) {
        ongoingCall?.let { call ->
            // Configurar ruta de audio (altavoz, auricular, etc.)
        }
    }
}
