package com.example.noya

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Protege el manifest: permisos y componentes que la app necesita para funcionar.
// Si al limpiar se borra algo necesario, esta prueba falla.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ManifestTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun declaraLosPermisosNecesarios() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions?.toSet().orEmpty()

        val required = listOf(
            Manifest.permission.CALL_PHONE, // hacer llamadas
            Manifest.permission.READ_CONTACTS, // lista de contactos
            Manifest.permission.WRITE_CONTACTS, // nuevo contacto
            Manifest.permission.READ_CALL_LOG, // llamadas perdidas
            Manifest.permission.ACCESS_NOTIFICATION_POLICY, // botón silenciar
            Manifest.permission.WAKE_LOCK, // encender pantalla y sensor de proximidad
            Manifest.permission.USE_FULL_SCREEN_INTENT, // notificación de llamada entrante
            Manifest.permission.POST_NOTIFICATIONS, // notificación de llamada entrante
            Manifest.permission.WRITE_SETTINGS, // brillo
            Manifest.permission.USE_EXACT_ALARM // devolver el sonido a las 2 horas
        )
        for (permission in required) {
            assertTrue("Falta el permiso $permission", permission in requested)
        }
    }

    @Test
    fun servicioDeLlamadasDeclaradoConPermisoDelSistema() {
        val component = ComponentName(context, NoyaInCallService::class.java)
        @Suppress("DEPRECATION")
        val info = context.packageManager.getServiceInfo(component, PackageManager.GET_META_DATA)
        assertEquals("android.permission.BIND_INCALL_SERVICE", info.permission)
        assertTrue(info.metaData.getBoolean("android.telecom.IN_CALL_SERVICE_UI"))
    }

    @Test
    fun receptoresDeclarados() {
        for (receiver in listOf(SilentModeRestoreReceiver::class.java, IncomingCallActionReceiver::class.java)) {
            val component = ComponentName(context, receiver)
            val info = context.packageManager.getReceiverInfo(component, 0)
            assertTrue("${receiver.simpleName} no debe ser exportado", !info.exported)
        }
    }

    @Test
    fun mainActivityEsPantallaDeInicioYMarcador() {
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:5551234"))
        )
        for (intent in intents) {
            intent.setPackage(context.packageName)
            val matches = context.packageManager.queryIntentActivities(intent, 0)
            assertTrue(
                "MainActivity no responde a $intent",
                matches.any { it.activityInfo.name == MainActivity::class.java.name }
            )
        }
    }
}
