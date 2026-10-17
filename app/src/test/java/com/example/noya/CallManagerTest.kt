package com.example.noya

import android.telecom.Call
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Manejo de llamadas: cuál suena, con cuál se habla y la llamada en espera.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CallManagerTest {

    private fun add(number: String, state: Int): FakeCall = TestState.addCall(number, state)

    @After
    fun tearDown() {
        TestState.reset()
    }

    @Test
    fun sinLlamadasElEstadoEsDesconectado() {
        assertEquals(0, CallManager.callCount())
        assertEquals(Call.STATE_DISCONNECTED, CallManager.callState.value)
        assertNull(CallManager.ongoingCall)
        assertNull(CallManager.activeCallNumber.value)
    }

    @Test
    fun llamadaEntranteSuena() {
        val ringing = add("5551111", Call.STATE_RINGING)

        assertSame(ringing.call, CallManager.ringingCall())
        assertNull(CallManager.activeCall())
        assertEquals(Call.STATE_RINGING, CallManager.callState.value)
        assertSame(ringing.call, CallManager.ongoingCall)
    }

    @Test
    fun contestarYRechazarActuanSobreLaLlamadaQueSuena() {
        val active = add("5551111", Call.STATE_ACTIVE)
        val ringing = add("5552222", Call.STATE_RINGING)

        CallManager.answerCall()
        verify { ringing.call.answer(0) }
        verify(exactly = 0) { active.call.answer(any()) }

        CallManager.rejectCall()
        verify { ringing.call.reject(false, null) }
        verify(exactly = 0) { active.call.reject(any<Boolean>(), any()) }
    }

    @Test
    fun colgarActuaSobreLaLlamadaActiva() {
        val active = add("5551111", Call.STATE_ACTIVE)
        val ringing = add("5552222", Call.STATE_RINGING)

        CallManager.endCall()
        verify { active.call.disconnect() }
        verify(exactly = 0) { ringing.call.disconnect() }
    }

    @Test
    fun llamadaSalienteMarcandoCuentaComoActiva() {
        val dialing = add("5553333", Call.STATE_DIALING)

        assertSame(dialing.call, CallManager.activeCall())
        assertEquals(Call.STATE_DIALING, CallManager.callState.value)
        assertEquals("5553333", CallManager.activeCallNumber.value)
    }

    @Test
    fun llamadaEnEsperaLaQueSuenaTienePrioridadEnElEstado() {
        add("5551111", Call.STATE_ACTIVE)
        add("5552222", Call.STATE_RINGING)

        assertEquals(Call.STATE_RINGING, CallManager.callState.value)
        // Se sigue hablando con la primera
        assertEquals("5551111", CallManager.activeCallNumber.value)
    }

    @Test
    fun alTerminarLaSegundaLlamadaSeReanudaLaQueEstabaEnEspera() {
        val first = add("5551111", Call.STATE_HOLDING)
        val second = add("5552222", Call.STATE_ACTIVE)
        assertEquals("5552222", CallManager.activeCallNumber.value)

        second.currentState = Call.STATE_DISCONNECTED
        CallManager.removeCall(second.call)

        verify { first.call.unhold() }
        assertEquals(1, CallManager.callCount())
        assertEquals("5551111", CallManager.activeCallNumber.value)
    }

    @Test
    fun noSeReanudaLaLlamadaEnEsperaSiOtraEstaSonando() {
        val held = add("5551111", Call.STATE_HOLDING)
        val active = add("5552222", Call.STATE_ACTIVE)
        add("5553333", Call.STATE_RINGING)

        active.currentState = Call.STATE_DISCONNECTED
        CallManager.removeCall(active.call)

        verify(exactly = 0) { held.call.unhold() }
    }

    @Test
    fun alTerminarLaUltimaLlamadaElEstadoVuelveADesconectado() {
        val only = add("5551111", Call.STATE_ACTIVE)

        only.currentState = Call.STATE_DISCONNECTED
        CallManager.removeCall(only.call)

        assertEquals(0, CallManager.callCount())
        assertEquals(Call.STATE_DISCONNECTED, CallManager.callState.value)
        assertNull(CallManager.activeCallNumber.value)
    }

    @Test
    fun agregarLaMismaLlamadaDosVecesNoLaDuplica() {
        val call = add("5551111", Call.STATE_ACTIVE)
        CallManager.addCall(call.call)

        assertEquals(1, CallManager.callCount())
    }
}
