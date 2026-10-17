package com.example.noya

import android.Manifest
import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.provider.CallLog
import android.telecom.Call
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.noya.data.getContacts
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Pantalla de inicio, contactos y el paso entre pantallas.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MainScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private lateinit var contacts: FakeContactsProvider
    private lateinit var callLog: FakeCallLogProvider

    @Before
    fun setUp() {
        TestState.reset()
        contacts = FakeContactsProvider.install()
        callLog = FakeCallLogProvider.install()
        shadowOf(context as Application).grantPermissions(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS
        )
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }

    @After
    fun tearDown() {
        TestState.reset()
    }

    private fun addContacts(count: Int) {
        repeat(count) { i ->
            contacts.rows.add(FakeContactsProvider.Row("$i", "Contacto %02d".format(i), "55500000%02d".format(i)))
        }
        // Actualiza la caché de contactos con estos datos
        getContacts(context, forceRefresh = true)
    }

    private fun showApp() {
        composeRule.setContent { MainScreen() }
        composeRule.waitForIdle()
    }

    // ---------- Inicio ----------

    @Test
    fun inicioMuestraBotonesPrincipales() {
        showApp()

        composeRule.onNodeWithText("Llamar").assertIsDisplayed()
        composeRule.onNodeWithText("Silenciar").assertIsDisplayed()
        composeRule.onNodeWithText("Llamadas Perdidas").assertIsDisplayed()
        composeRule.onNodeWithText("No hay llamadas perdidas").assertIsDisplayed()
    }

    @Test
    fun botonSilenciarMuestraElEstadoRealDelTelefono() {
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        showApp()

        composeRule.onNodeWithText("Sonido").assertIsDisplayed()
    }

    @Test
    fun vibracionCuentaComoSilencio() {
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        showApp()

        composeRule.onNodeWithText("Sonido").assertIsDisplayed()
    }

    @Test
    fun inicioMuestraLlamadasPerdidasConNombre() {
        contacts.rows.add(FakeContactsProvider.Row("1", "Juan", "5551111111"))
        callLog.rows.add(FakeCallLogProvider.Row("1", "5551111111", System.currentTimeMillis() - 60_000, CallLog.Calls.MISSED_TYPE))
        showApp()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Juan") }
    }

    // ---------- Contactos ----------

    @Test
    fun botonLlamarAbreLosContactosConIndicadorDePagina() {
        addContacts(20)
        showApp()

        composeRule.onNodeWithText("Llamar").performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Contacto 00") }
        composeRule.onNodeWithText("Página 1 de", substring = true).assertIsDisplayed()
    }

    @Test
    fun flechasCambianDePagina() {
        addContacts(20)
        showApp()
        composeRule.onNodeWithText("Llamar").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Contacto 00") }

        composeRule.onNodeWithContentDescription("Página siguiente").performClick()
        composeRule.onNodeWithText("Página 2 de", substring = true).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Página anterior").performClick()
        composeRule.onNodeWithText("Página 1 de", substring = true).assertIsDisplayed()
    }

    @Test
    fun conPocosContactosHayUnaSolaPagina() {
        addContacts(1)
        showApp()
        composeRule.onNodeWithText("Llamar").performClick()

        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTextExists("Contacto 00") }
        composeRule.onNodeWithText("Página 1 de 1").assertIsDisplayed()
    }

    // ---------- Llamada entrante desde el inicio ----------

    @Test
    fun llamadaEntranteAbreSuPantallaYAlContestarMuestraColgar() {
        val ringing = TestState.addCall("5551111111", Call.STATE_RINGING)
        showApp()

        IncomingCallState.incomingCallNumber.value = "5551111111"
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Llamada entrante").assertIsDisplayed()

        composeRule.onNodeWithText("Aceptar").performClick()
        verify { ringing.call.answer(0) }
        // El sistema avisa que ya se contestó
        ringing.currentState = Call.STATE_ACTIVE
        CallManager.updateState()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Colgar").assertIsDisplayed()
    }

    @Test
    fun rechazarSinOtraLlamadaRegresaAlInicio() {
        val ringing = TestState.addCall("5551111111", Call.STATE_RINGING)
        showApp()
        IncomingCallState.incomingCallNumber.value = "5551111111"
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Rechazar").performClick()
        verify { ringing.call.reject(false, null) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Llamar").assertIsDisplayed()
    }

    @Test
    fun rechazarConOtraLlamadaActivaVuelveAEsaLlamada() {
        TestState.addCall("5552222222", Call.STATE_ACTIVE)
        val ringing = TestState.addCall("5551111111", Call.STATE_RINGING)
        showApp()
        IncomingCallState.incomingCallNumber.value = "5551111111"
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Rechazar").performClick()
        verify { ringing.call.reject(false, null) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Colgar").assertIsDisplayed()
    }

    @Test
    fun contestarDesdeLaNotificacionAbreLaLlamada() {
        val ringing = TestState.addCall("5551111111", Call.STATE_RINGING)
        showApp()

        IncomingCallState.incomingCallNumber.value = "5551111111"
        IncomingCallState.answerRequested.value = true
        composeRule.waitForIdle()

        verify { ringing.call.answer(0) }
        composeRule.onNodeWithText("Colgar").assertIsDisplayed()
    }

    @Test
    fun alTerminarLaLlamadaRegresaAlInicio() {
        val active = TestState.addCall("5551111111", Call.STATE_RINGING)
        showApp()
        IncomingCallState.incomingCallNumber.value = "5551111111"
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Aceptar").performClick()
        active.currentState = Call.STATE_ACTIVE
        CallManager.updateState()
        composeRule.waitForIdle()

        // El otro cuelga
        active.currentState = Call.STATE_DISCONNECTED
        CallManager.removeCall(active.call)
        IncomingCallState.callEnded.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Llamar").assertIsDisplayed()
    }

    @Test
    fun alVolverDeSegundoPlanoRegresaAlInicio() {
        addContacts(1)
        showApp()
        composeRule.onNodeWithText("Llamar").performClick()
        composeRule.waitForIdle()

        ScreenNavigationState.shouldNavigateToHome.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Silenciar").assertIsDisplayed()
    }
}
