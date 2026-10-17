package com.example.noya

import android.telecom.Call
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.noya.data.Contact
import com.example.noya.ui.ProvideResponsiveDimens
import com.example.noya.ui.screens.ActiveCallScreen
import com.example.noya.ui.screens.IncomingCallScreen
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Pantallas de llamada entrante y llamada en curso.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CallScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var contacts: FakeContactsProvider

    @Before
    fun setUp() {
        TestState.reset()
        contacts = FakeContactsProvider.install()
        contacts.rows.add(FakeContactsProvider.Row("1", "Juan", "5551111111"))
    }

    @After
    fun tearDown() {
        TestState.reset()
    }

    // ---------- Llamada entrante ----------

    private var accepted = 0
    private var rejected = 0
    private var gone = 0

    private fun showIncoming(number: String) {
        composeRule.setContent {
            ProvideResponsiveDimens {
                IncomingCallScreen(
                    callerNumber = number,
                    onAccept = { accepted++ },
                    onReject = { rejected++ },
                    onCallGone = { gone++ }
                )
            }
        }
    }

    @Test
    fun llamadaEntranteMuestraNombreYBotones() {
        TestState.addCall("5551111111", Call.STATE_RINGING)
        showIncoming("5551111111")

        composeRule.onNodeWithText("Llamada entrante").assertIsDisplayed()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTextExists("Juan")
        }
        composeRule.onNodeWithText("Aceptar").assertIsDisplayed()
        composeRule.onNodeWithText("Rechazar").assertIsDisplayed()
    }

    @Test
    fun botonesAceptarYRechazar() {
        TestState.addCall("5551111111", Call.STATE_RINGING)
        showIncoming("5551111111")

        composeRule.onNodeWithText("Aceptar").performClick()
        assertEquals(1, accepted)

        composeRule.onNodeWithText("Rechazar").performClick()
        assertEquals(1, rejected)
        assertEquals(0, gone)
    }

    @Test
    fun siCuelganAntesDeContestarSeCierraLaPantalla() {
        val ringing = TestState.addCall("5551111111", Call.STATE_RINGING)
        showIncoming("5551111111")
        composeRule.waitForIdle()
        assertEquals(0, gone)

        ringing.currentState = Call.STATE_DISCONNECTED
        CallManager.removeCall(ringing.call)
        composeRule.waitForIdle()

        assertEquals(1, gone)
    }

    @Test
    fun conLlamadaEnEsperaSeCierraCuandoDejaDeSonarAunqueHayaOtraActiva() {
        TestState.addCall("5552222222", Call.STATE_ACTIVE)
        val ringing = TestState.addCall("5551111111", Call.STATE_RINGING)
        showIncoming("5551111111")
        composeRule.waitForIdle()

        ringing.currentState = Call.STATE_DISCONNECTED
        CallManager.removeCall(ringing.call)
        composeRule.waitForIdle()

        assertEquals(1, gone)
    }

    // ---------- Llamada en curso ----------

    private var ended = 0

    private fun showActive(contact: Contact) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ProvideResponsiveDimens {
                ActiveCallScreen(contact = contact, onEndCall = { ended++ })
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    @Test
    fun llamadaSalienteNoSeCierraAlAbrirLaPantalla() {
        // Antes de marcar, el estado es "desconectada": no debe cerrar la pantalla
        showActive(Contact("1", "Juan", "5551111111"))

        composeRule.onNodeWithText("Colgar").assertIsDisplayed()
        composeRule.onNodeWithText("Más Volumen").assertIsDisplayed()
        assertEquals(0, ended)
    }

    @Test
    fun llamadaSalienteQueNuncaEmpiezaSeCierraALos5Segundos() {
        showActive(Contact("1", "Juan", "5551111111"))

        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.waitForIdle()
        assertEquals(0, ended)

        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, ended)
    }

    @Test
    fun alColgarSeTerminaLaLlamada() {
        val active = TestState.addCall("5551111111", Call.STATE_ACTIVE)
        showActive(Contact("1", "Juan", "5551111111"))

        composeRule.onNodeWithText("Colgar").performClick()

        verify { active.call.disconnect() }
        assertEquals(1, ended)
    }

    @Test
    fun alColgarConOtraLlamadaEnEsperaNoSeSaleDeLaPantalla() {
        TestState.addCall("5552222222", Call.STATE_HOLDING)
        val active = TestState.addCall("5551111111", Call.STATE_ACTIVE)
        showActive(Contact("1", "Juan", "5551111111"))

        composeRule.onNodeWithText("Colgar").performClick()

        verify { active.call.disconnect() }
        assertEquals(0, ended)
    }

    @Test
    fun cuandoElOtroCuelgaSeCierraLaPantalla() {
        val active = TestState.addCall("5551111111", Call.STATE_ACTIVE)
        showActive(Contact("1", "Juan", "5551111111"))
        assertEquals(0, ended)

        active.currentState = Call.STATE_DISCONNECTED
        CallManager.removeCall(active.call)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()

        assertEquals(1, ended)
    }

    @Test
    fun llamadaContestadaMuestraElNombreDelContacto() {
        TestState.addCall("5551111111", Call.STATE_ACTIVE)
        // Las llamadas entrantes solo traen el número
        showActive(Contact("incoming", "5551111111", "5551111111"))

        composeRule.mainClock.autoAdvance = true
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Juan") }
    }
}
