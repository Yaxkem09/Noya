package com.example.noya.ui.screens

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.telecom.Call
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.noya.CallManager
import com.example.noya.data.Contact
import com.example.noya.data.getContactInfoFromNumber
import com.example.noya.device.makePhoneCall
import com.example.noya.ui.ResponsiveDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ActiveCallScreen(contact: Contact, onEndCall: () -> Unit) {
    val context = LocalContext.current
    var isSpeakerOn by remember { mutableStateOf(false) }
    val callState by CallManager.callState.collectAsStateWithLifecycle()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var hasEnded by remember { mutableStateOf(false) }
    // Solo se considera que la llamada terminó si antes se vio iniciada
    // (el estado inicial de CallManager es DISCONNECTED cuando no hay llamada)
    var callStarted by remember { mutableStateOf(false) }
    var contactPhoto by remember { mutableStateOf<String?>(contact.photoUri) }
    var contactName by remember { mutableStateOf(contact.name) }

    // Obtener nombre y foto del contacto si no los tenemos (en IO thread).
    // Las llamadas entrantes solo traen el número.
    LaunchedEffect(contact.phoneNumber) {
        if (contactPhoto == null || contact.id == "incoming") {
            val (name, photoUri) = withContext(Dispatchers.IO) {
                getContactInfoFromNumber(context, contact.phoneNumber)
            }
            if (name != null) contactName = name
            if (contactPhoto == null) contactPhoto = photoUri
        }
    }

    // Monitorear el estado de la llamada con Flow compartido
    LaunchedEffect(callState) {
        Log.d("ActiveCallScreen", "Estado de llamada actualizado: $callState")
        if (callState == Call.STATE_DIALING ||
            callState == Call.STATE_CONNECTING ||
            callState == Call.STATE_RINGING ||
            callState == Call.STATE_ACTIVE ||
            callState == Call.STATE_HOLDING
        ) {
            callStarted = true
        }
        if (callState == Call.STATE_DISCONNECTED && callStarted && !hasEnded) {
            Log.d("ActiveCallScreen", "Llamada desconectada - cerrando pantalla")
            hasEnded = true
            CallManager.setSpeaker(false)
            onEndCall()
        }
    }

    // Verificación periódica: si no hay llamada activa después de un tiempo, cerrar
    // Solo para llamadas entrantes que fueron aceptadas - las salientes tardan en establecerse
    LaunchedEffect(Unit) {
        // Para llamadas salientes, dar más tiempo para que se establezca
        val isOutgoingCall = contact.id != "incoming"
        val waitTime = if (isOutgoingCall) 5000L else 500L

        delay(waitTime)

        val currentCall = CallManager.ongoingCall
        if (currentCall == null && !hasEnded) {
            Log.d("ActiveCallScreen", "No hay llamada activa después de esperar - cerrando pantalla")
            hasEnded = true
            CallManager.setSpeaker(false)
            onEndCall()
        }
    }

    // Iniciar la llamada cuando se muestra la pantalla (solo para llamadas salientes)
    // El modo de audio lo maneja el sistema (Telecom); no se modifica aquí
    LaunchedEffect(contact.id) {
        // Solo iniciar llamada si no hay una llamada en curso
        if (CallManager.ongoingCall == null && contact.id != "incoming") {
            makePhoneCall(context, contact.phoneNumber)
        }
    }

    // Mantener la pantalla encendida y limpiar al salir
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            Log.d("ActiveCallScreen", "Pantalla de llamada destruida - limpiando recursos")
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Limpiar configuración de audio
            try {
                CallManager.setSpeaker(false)
            } catch (e: Exception) {
                Log.e("ActiveCallScreen", "Error al restaurar audio: ${e.message}", e)
            }

        }
    }

    // Sensor de proximidad: apagar la pantalla al acercar el teléfono a la oreja
    // para evitar toques accidentales con la mejilla. Se desactiva con el altavoz.
    DisposableEffect(isSpeakerOn) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val proximityWakeLock =
            if (!isSpeakerOn && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "noya:proximity").apply {
                    setReferenceCounted(false)
                    acquire(4 * 60 * 60 * 1000L) // 4 horas máximo
                }
            } else {
                null
            }
        Log.d("ActiveCallScreen", "Sensor de proximidad ${if (proximityWakeLock != null) "activado" else "desactivado"}")

        onDispose {
            // Esperar a que se aleje el teléfono de la cara antes de encender la pantalla
            if (proximityWakeLock?.isHeld == true) {
                proximityWakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
            }
        }
    }

    // Dimensiones responsivas
    val screenPadding = ResponsiveDimens.current.screenPadding
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val largePadding = ResponsiveDimens.current.largePadding
    val callPhotoSize = ResponsiveDimens.current.callPhotoSize
    val largeCornerRadius = ResponsiveDimens.current.largeCornerRadius
    val largeTextSize = ResponsiveDimens.current.largeTextSize
    val mediumTextSize = ResponsiveDimens.current.mediumTextSize
    val largeIconSize = ResponsiveDimens.current.largeIconSize
    val largeButtonHeight = ResponsiveDimens.current.largeButtonHeight
    val scaleFactor = ResponsiveDimens.current.scaleFactor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Foto del contacto o icono por defecto
        Box(
            modifier = Modifier
                .size(callPhotoSize)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(largeCornerRadius))
                .background(Color(0xFFE8E8E8)),
            contentAlignment = Alignment.Center
        ) {
            if (contactPhoto != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(contactPhoto)
                        .crossfade(true)
                        .size(200) // Limitar tamaño de decodificación
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Foto de $contactName",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(largeCornerRadius)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Sin foto",
                    modifier = Modifier.size((80 * scaleFactor).dp),
                    tint = Color(0xFF58D68D)
                )
            }
        }

        Spacer(modifier = Modifier.height(largePadding))

        // Nombre del contacto
        Text(
            text = contactName,
            fontSize = (36 * scaleFactor).sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50), // Azul oscuro suave
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(mediumPadding))

        // Número de teléfono
        Text(
            text = contact.phoneNumber,
            fontSize = mediumTextSize.sp,
            color = Color(0xFF5D6D7E), // Gris azulado
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(largePadding))

        // Estado de llamada
        Text(
            text = "En llamada...",
            fontSize = largeTextSize.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF85929E), // Gris suave
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Botón de más volumen (altavoz)
        Button(
            onClick = {
                isSpeakerOn = !isSpeakerOn

                try {
                    // Usar InCallService para cambiar ruta de audio (forma correcta)
                    CallManager.setSpeaker(isSpeakerOn)

                    if (isSpeakerOn) {
                        // Subir volumen de comunicación al máximo
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
                    }

                    Toast.makeText(
                        context,
                        if (isSpeakerOn) "Altavoz activado" else "Altavoz desactivado",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Log.e("ActiveCallScreen", "Error al cambiar altavoz: ${e.message}", e)
                    Toast.makeText(context, "Error al cambiar altavoz", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(largeButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSpeakerOn) Color(0xFF5DADE2) else Color(0xFF85929E), // Azul claro o gris suave
                contentColor = Color.White
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Más volumen",
                    modifier = Modifier.size(largeIconSize)
                )
                Spacer(modifier = Modifier.width(mediumPadding))
                Text(
                    text = if (isSpeakerOn) "Volumen Normal" else "Más Volumen",
                    fontSize = mediumTextSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(largePadding))

        // Botón para colgar
        Button(
            onClick = {
                // Si hay otra llamada en espera, no salir de la pantalla:
                // se reanuda y la pantalla muestra sus datos
                val hasOtherCall = CallManager.callCount() > 1
                CallManager.setSpeaker(false)
                CallManager.endCall()
                if (!hasOtherCall) {
                    onEndCall()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(largeButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEC7063), // Rojo más suave y menos agresivo
                contentColor = Color.White
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "Colgar",
                    modifier = Modifier.size(largeIconSize)
                )
                Spacer(modifier = Modifier.width(mediumPadding))
                Text(
                    text = "Colgar",
                    fontSize = mediumTextSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height((48 * scaleFactor).dp))
    }
}
