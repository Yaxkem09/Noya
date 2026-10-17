package com.example.noya

import android.content.Context
import android.provider.CallLog
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.noya.data.clearContactInfoCache
import com.example.noya.data.getMissedCalls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Panel de llamadas perdidas: cuáles se muestran y con qué nombre.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MissedCallsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var contacts: FakeContactsProvider
    private lateinit var callLog: FakeCallLogProvider

    private val hour = 60 * 60 * 1000L
    private val now = System.currentTimeMillis()
    private var nextId = 1

    @Before
    fun setUp() {
        contacts = FakeContactsProvider.install()
        callLog = FakeCallLogProvider.install()
        clearContactInfoCache()
    }

    private fun missed(number: String, hoursAgo: Double) {
        callLog.rows.add(FakeCallLogProvider.Row((nextId++).toString(), number, now - (hoursAgo * hour).toLong(), CallLog.Calls.MISSED_TYPE))
    }

    private fun outgoing(number: String, hoursAgo: Double) {
        callLog.rows.add(FakeCallLogProvider.Row((nextId++).toString(), number, now - (hoursAgo * hour).toLong(), CallLog.Calls.OUTGOING_TYPE))
    }

    @Test
    fun muestraLlamadaPerdidaConNombreDelContacto() {
        contacts.rows.add(FakeContactsProvider.Row("1", "Juan", "555 111 1111", "content://foto/juan"))
        missed("5551111111", 1.0)

        val calls = getMissedCalls(context)

        assertEquals(1, calls.size)
        assertEquals("Juan", calls[0].name)
        assertEquals("5551111111", calls[0].phoneNumber)
        assertEquals("content://foto/juan", calls[0].photoUri)
    }

    @Test
    fun numeroDesconocidoNoTieneNombre() {
        missed("5559999999", 1.0)

        val calls = getMissedCalls(context)

        assertEquals(1, calls.size)
        assertNull(calls[0].name)
    }

    @Test
    fun ocultaLasLlamadasYaDevueltas() {
        missed("5552222222", 2.0)
        outgoing("555-222-2222", 1.0) // devuelta después, con otro formato

        assertTrue(getMissedCalls(context).isEmpty())
    }

    @Test
    fun unaLlamadaSalienteAnteriorNoCuentaComoDevuelta() {
        outgoing("5553333333", 4.0)
        missed("5553333333", 3.0)

        assertEquals(1, getMissedCalls(context).size)
    }

    @Test
    fun soloUnaEntradaPorNumeroLaMasReciente() {
        missed("5551111111", 5.0)
        missed("5551111111", 1.0)

        val calls = getMissedCalls(context)

        assertEquals(1, calls.size)
        // La más reciente es la de hace 1 hora (id 2)
        assertEquals("2", calls[0].id)
    }

    @Test
    fun ignoraLlamadasDeHaceMasDe24Horas() {
        missed("5554444444", 30.0)

        assertTrue(getMissedCalls(context).isEmpty())
    }

    @Test
    fun ordenaDeLaMasRecienteALaMasAntigua() {
        missed("5551111111", 5.0)
        missed("5552222222", 1.0)
        missed("5553333333", 3.0)

        val numbers = getMissedCalls(context).map { it.phoneNumber }

        assertEquals(listOf("5552222222", "5553333333", "5551111111"), numbers)
    }

    @Test
    fun alLimpiarLaCacheApareceElNombreDeUnContactoNuevo() {
        missed("5557777777", 1.0)
        assertNull(getMissedCalls(context)[0].name)

        // Se guarda el contacto después
        contacts.rows.add(FakeContactsProvider.Row("7", "María", "5557777777"))
        clearContactInfoCache()

        assertEquals("María", getMissedCalls(context)[0].name)
    }
}
