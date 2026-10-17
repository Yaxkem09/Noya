package com.example.noya

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.noya.ui.ProvideResponsiveDimens
import com.example.noya.ui.screens.AdvancedOptionsScreen
import com.example.noya.ui.screens.NewContactScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Opciones avanzadas, nuevo contacto y configuraciones guardadas.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi") // tamaño de un teléfono típico
class OtherScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        TestState.reset()
        context.getSharedPreferences("noya_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        TestState.reset()
    }

    // Simula si la app tiene el permiso "Modificar ajustes del sistema"
    private fun setCanWriteSettings(allowed: Boolean) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        shadowOf(appOps).setMode(
            AppOpsManager.OPSTR_WRITE_SETTINGS,
            Process.myUid(),
            context.packageName,
            if (allowed) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_ERRORED
        )
    }

    @Test
    fun configuracionesSeGuardanYSeRecuperan() {
        AppSettings.init(context)
        AppSettings.hideContactNames.value = true
        AppSettings.hideContactPhotos.value = true
        AppSettings.contactPhotoSize.floatValue = 150f
        AppSettings.save()

        // Simula reiniciar la app
        TestState.reset()
        AppSettings.init(context)

        assertEquals(true, AppSettings.hideContactNames.value)
        assertEquals(true, AppSettings.hideContactPhotos.value)
        assertEquals(150f, AppSettings.contactPhotoSize.floatValue)
    }

    private fun showAdvancedOptions() {
        composeRule.setContent {
            ProvideResponsiveDimens {
                AdvancedOptionsScreen(onNavigateBack = {}, onNavigateToNewContact = {})
            }
        }
    }

    @Test
    fun sinPermisoDeAjustesMuestraBotonEnVezDeDeslizadorDeBrillo() {
        setCanWriteSettings(false)
        showAdvancedOptions()

        composeRule.onNodeWithText("Dar permiso para cambiar brillo").assertIsDisplayed()
        assertEquals(false, composeRule.onAllNodesWithTextExists("Brillo de pantalla", substring = true))
    }

    @Test
    fun conPermisoDeAjustesMuestraElDeslizadorDeBrillo() {
        setCanWriteSettings(true)
        showAdvancedOptions()

        assertEquals(true, composeRule.onAllNodesWithTextExists("Brillo de pantalla", substring = true))
        assertEquals(false, composeRule.onAllNodesWithTextExists("Dar permiso para cambiar brillo"))
    }

    @Test
    fun opcionesAvanzadasMuestraSusBotones() {
        showAdvancedOptions()

        composeRule.onNodeWithText("Regresar").assertIsDisplayed()
        composeRule.onNodeWithText("Nuevo Contacto").assertIsDisplayed()
        composeRule.onNodeWithText("Configuraciones del Teléfono").assertIsDisplayed()
        composeRule.onNodeWithText("Ocultar nombres de contactos").assertIsDisplayed()
        composeRule.onNodeWithText("Ocultar fotos de contactos").assertIsDisplayed()
    }

    @Test
    fun tecladoDeNuevoContactoEscribeYBorraElNumero() {
        composeRule.setContent {
            ProvideResponsiveDimens {
                NewContactScreen(onNavigateBack = {})
            }
        }

        composeRule.onNodeWithText("Número de teléfono").assertIsDisplayed()
        // Dígitos distintos: el número escrito no se confunde con las teclas
        composeRule.onNodeWithText("5").performClick()
        composeRule.onNodeWithText("1").performClick()
        composeRule.onNodeWithText("2").performClick()
        composeRule.onNodeWithText("512").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Borrar").performClick()
        composeRule.onNodeWithText("51").assertIsDisplayed()
    }
}
