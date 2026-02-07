package com.example.noya

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class NoyaInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("NoyaInCallService", "Servicio creado - ESPERANDO LLAMADAS")
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        Log.d("NoyaInCallService", "Servicio enlazado (onBind)")
        return super.onBind(intent)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val callerNumber = call.details.handle?.schemeSpecificPart ?: "Desconocido"
        val callState = getStateString(call.state)
        Log.d("NoyaInCallService", "========================================")
        Log.d("NoyaInCallService", "LLAMADA AÑADIDA")
        Log.d("NoyaInCallService", "Número: $callerNumber")
        Log.d("NoyaInCallService", "Estado: $callState (${call.state})")
        Log.d("NoyaInCallService", "========================================")

        // Limpiar estado de llamadas anteriores antes de asignar la nueva
        IncomingCallState.callEnded.value = false

        // Notificar que hay una llamada activa (thread-safe)
        CallManager.ongoingCall = call
        CallManager.setCallState(call.state)

        // Si es una llamada entrante (ringing), mostrar la UI
        if (call.state == Call.STATE_RINGING) {
            Log.d("NoyaInCallService", "Mostrando UI de llamada entrante")

            // Lanzar la actividad principal para mostrar la llamada entrante
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                putExtra("INCOMING_CALL", true)
                putExtra("CALLER_NUMBER", callerNumber)
            }
            startActivity(intent)
        }

        // Agregar listener para cambios de estado
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                val stateString = getStateString(state)
                Log.d("NoyaInCallService", "Cambio de estado: $stateString ($state)")
                CallManager.setCallState(state)

                if (state == Call.STATE_DISCONNECTED) {
                    Log.d("NoyaInCallService", "Llamada DESCONECTADA - limpiando estado")
                    CallManager.ongoingCall = null
                    IncomingCallState.callEnded.value = true
                }
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("NoyaInCallService", "========================================")
        Log.d("NoyaInCallService", "LLAMADA REMOVIDA")
        Log.d("NoyaInCallService", "========================================")
        CallManager.ongoingCall = null
        CallManager.setCallState(Call.STATE_DISCONNECTED)
        IncomingCallState.callEnded.value = true
    }

    private fun getStateString(state: Int): String {
        return when (state) {
            Call.STATE_NEW -> "NUEVA"
            Call.STATE_RINGING -> "TIMBRANDO"
            Call.STATE_DIALING -> "MARCANDO"
            Call.STATE_ACTIVE -> "ACTIVA"
            Call.STATE_HOLDING -> "EN ESPERA"
            Call.STATE_DISCONNECTED -> "DESCONECTADA"
            Call.STATE_CONNECTING -> "CONECTANDO"
            Call.STATE_DISCONNECTING -> "DESCONECTANDO"
            else -> "DESCONOCIDO"
        }
    }
}

object CallManager {
    @Volatile
    var ongoingCall: Call? = null

    @Volatile
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
