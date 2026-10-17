package com.example.noya

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Devolver el sonido automáticamente después de 2 horas en silencio.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SilentModeTimerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences("noya_settings", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
    }

    @Test
    fun laDuracionEsDeDosHoras() {
        assertEquals(2 * 60 * 60 * 1000L, SilentModeTimer.SILENT_DURATION_MS)
    }

    @Test
    fun alSilenciarSeProgramaLaAlarmaEnDosHoras() {
        val before = System.currentTimeMillis()
        SilentModeTimer.ensureScheduled(context)

        val alarm = shadowOf(alarmManager).nextScheduledAlarm
        assertNotNull("No se programó la alarma", alarm)
        val expected = before + SilentModeTimer.SILENT_DURATION_MS
        assertTrue(
            "La alarma debe ser en ~2 horas",
            alarm!!.triggerAtTime in expected..(expected + 5_000)
        )
    }

    @Test
    fun programarDeNuevoNoReiniciaElConteo() {
        SilentModeTimer.ensureScheduled(context)
        val first = shadowOf(alarmManager).nextScheduledAlarm!!.triggerAtTime

        Thread.sleep(20)
        SilentModeTimer.ensureScheduled(context)
        val second = shadowOf(alarmManager).nextScheduledAlarm!!.triggerAtTime

        assertEquals(first, second)
    }

    @Test
    fun alQuitarElSilencioSeCancelaLaAlarma() {
        SilentModeTimer.ensureScheduled(context)
        SilentModeTimer.cancel(context)

        assertNull(shadowOf(alarmManager).nextScheduledAlarm)
    }

    @Test
    fun laAlarmaDevuelveElSonidoConPermisoDeNoMolestar() {
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

        SilentModeRestoreReceiver().onReceive(context, null)

        assertEquals(AudioManager.RINGER_MODE_NORMAL, audioManager.ringerMode)
    }

    @Test
    fun sinPermisoDeNoMolestarNoCambiaElSonido() {
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(false)
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

        SilentModeTimer.restoreSound(context)

        assertEquals(AudioManager.RINGER_MODE_SILENT, audioManager.ringerMode)
    }
}
