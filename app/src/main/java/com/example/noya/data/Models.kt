package com.example.noya.data

import androidx.compose.runtime.Stable

@Stable
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

@Stable
data class CallLog(
    val id: String,
    val phoneNumber: String,
    val name: String?,
    val date: String,
    val time: String,
    val photoUri: String? = null
)

data class BatteryStatus(val level: Int, val isCharging: Boolean)
