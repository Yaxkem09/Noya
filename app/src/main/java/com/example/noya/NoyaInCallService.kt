package com.example.noya

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NoyaInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallManager.inCallService = this
        Log.d("NoyaInCallService", "Servicio creado - ESPERANDO LLAMADAS")
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.inCallService = null
        Log.d("NoyaInCallService", "Servicio destruido")
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

        // Limpiar estado de llamadas anteriores antes de agregar la nueva
        IncomingCallState.callEnded.value = false

        // Registrar la llamada (puede haber más de una: llamada en espera)
        CallManager.addCall(call)

        // Si es una llamada entrante (ringing), mostrar la UI
        if (call.state == Call.STATE_RINGING) {
            Log.d("NoyaInCallService", "Mostrando UI de llamada entrante")

            // Despertar el dispositivo completamente con WakeLock
            @Suppress("DEPRECATION")
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "noya:incomingCall"
            )
            wakeLock.acquire(10_000L) // 10 segundos máximo

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

                if (state == Call.STATE_DISCONNECTED) {
                    Log.d("NoyaInCallService", "Llamada DESCONECTADA - limpiando estado")
                    CallManager.removeCall(call)
                    // Solo regresar a home cuando ya no queda ninguna llamada
                    if (CallManager.callCount() == 0) {
                        IncomingCallState.callEnded.value = true
                    }
                } else {
                    CallManager.updateState()
                }
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("NoyaInCallService", "========================================")
        Log.d("NoyaInCallService", "LLAMADA REMOVIDA")
        Log.d("NoyaInCallService", "========================================")
        CallManager.removeCall(call)
        if (CallManager.callCount() == 0) {
            IncomingCallState.callEnded.value = true
        }
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

// Maneja todas las llamadas en curso. Puede haber más de una cuando entra
// una segunda llamada mientras se habla (llamada en espera).
// Los callbacks de InCallService llegan en el hilo principal, igual que la UI.
object CallManager {
    private val calls = mutableListOf<Call>()

    @Volatile
    var inCallService: NoyaInCallService? = null

    // Estado de la llamada principal (la que suena, o si no, la activa)
    private val _callState = MutableStateFlow(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState.asStateFlow()

    // Número de la llamada con la que se está hablando
    private val _activeCallNumber = MutableStateFlow<String?>(null)
    val activeCallNumber: StateFlow<String?> = _activeCallNumber.asStateFlow()

    // Llamada activa si existe, si no la que está sonando
    val ongoingCall: Call?
        get() = activeCall() ?: ringingCall()

    fun ringingCall(): Call? = calls.firstOrNull { it.state == Call.STATE_RINGING }

    // Llamada con la que se habla: activa, luego marcando/conectando, luego en espera
    fun activeCall(): Call? =
        calls.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: calls.firstOrNull {
                it.state == Call.STATE_DIALING ||
                    it.state == Call.STATE_CONNECTING ||
                    it.state == Call.STATE_NEW
            }
            ?: calls.firstOrNull { it.state == Call.STATE_HOLDING }

    fun callCount(): Int = calls.size

    fun addCall(call: Call) {
        if (call !in calls) calls.add(call)
        updateState()
    }

    fun removeCall(call: Call) {
        calls.remove(call)
        resumeHeldCall()
        updateState()
    }

    fun updateState() {
        val primary = ringingCall() ?: activeCall()
        _callState.value = primary?.state ?: Call.STATE_DISCONNECTED
        _activeCallNumber.value = activeCall()?.details?.handle?.schemeSpecificPart
    }

    // Si terminó la llamada activa y queda una en espera, reanudarla
    // para que no se quede en silencio
    private fun resumeHeldCall() {
        if (ringingCall() != null) return
        val hasActive = calls.any {
            it.state == Call.STATE_ACTIVE ||
                it.state == Call.STATE_DIALING ||
                it.state == Call.STATE_CONNECTING
        }
        if (!hasActive) {
            calls.firstOrNull { it.state == Call.STATE_HOLDING }?.let {
                Log.d("CallManager", "Reanudando llamada en espera")
                it.unhold()
            }
        }
    }

    fun endCall() {
        activeCall()?.disconnect()
    }

    fun answerCall() {
        ringingCall()?.answer(0) // 0 = video state (audio only)
    }

    fun rejectCall() {
        ringingCall()?.reject(false, null)
    }

    fun setSpeaker(enabled: Boolean) {
        try {
            val route = if (enabled) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            inCallService?.setAudioRoute(route)
            Log.d("CallManager", "Audio route cambiado a: ${if (enabled) "SPEAKER" else "EARPIECE"}")
        } catch (e: Exception) {
            Log.e("CallManager", "Error al cambiar ruta de audio: ${e.message}", e)
        }
    }

    fun isSpeakerOn(): Boolean {
        return inCallService?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
    }
}
