package com.example.noya

import android.net.Uri
import android.telecom.Call
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import com.example.noya.data.clearContactInfoCache
import io.mockk.every
import io.mockk.mockk

// ¿Hay algún elemento en pantalla con este texto?
fun ComposeContentTestRule.onAllNodesWithTextExists(text: String, substring: Boolean = false): Boolean =
    onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

// Llamada simulada cuyo estado se puede cambiar durante la prueba
class FakeCall(number: String, var currentState: Int) {
    val call: Call = mockk(relaxed = true)

    init {
        val details: Call.Details = mockk(relaxed = true)
        every { details.handle } returns Uri.fromParts("tel", number, null)
        every { call.details } returns details
        @Suppress("DEPRECATION")
        every { call.state } answers { currentState }
    }
}

// Los objetos globales sobreviven entre pruebas: dejarlos limpios
object TestState {
    private val addedCalls = mutableListOf<Call>()

    fun addCall(number: String, state: Int): FakeCall {
        val fake = FakeCall(number, state)
        addedCalls.add(fake.call)
        CallManager.addCall(fake.call)
        return fake
    }

    fun reset() {
        addedCalls.forEach { CallManager.removeCall(it) }
        addedCalls.clear()
        IncomingCallState.incomingCallNumber.value = null
        IncomingCallState.callEnded.value = false
        IncomingCallState.answerRequested.value = false
        ScreenNavigationState.shouldNavigateToHome.value = false
        AppSettings.hideContactNames.value = false
        AppSettings.hideContactPhotos.value = false
        AppSettings.contactPhotoSize.floatValue = 95f
        clearContactInfoCache()
    }
}
