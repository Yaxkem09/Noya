package com.example.noya

import android.content.Context
import androidx.compose.runtime.*

object IncomingCallState {
    val incomingCallNumber = mutableStateOf<String?>(null)
    val callEnded = mutableStateOf(false)
    // Se pidió contestar desde la notificación
    val answerRequested = mutableStateOf(false)
}

object ScreenNavigationState {
    var shouldNavigateToHome = mutableStateOf(false)
}

object AppSettings {
    private var prefs: android.content.SharedPreferences? = null

    var hideContactNames = mutableStateOf(false)
    var hideContactPhotos = mutableStateOf(false)
    var contactPhotoSize = mutableFloatStateOf(95f) // Tamaño por defecto en dp

    fun init(context: Context) {
        prefs = context.getSharedPreferences("noya_settings", Context.MODE_PRIVATE)
        prefs?.let {
            hideContactNames.value = it.getBoolean("hide_contact_names", false)
            hideContactPhotos.value = it.getBoolean("hide_contact_photos", false)
            contactPhotoSize.floatValue = it.getFloat("contact_photo_size", 95f)
        }
    }

    fun save() {
        prefs?.edit()
            ?.putBoolean("hide_contact_names", hideContactNames.value)
            ?.putBoolean("hide_contact_photos", hideContactPhotos.value)
            ?.putFloat("contact_photo_size", contactPhotoSize.floatValue)
            ?.apply()
    }
}
