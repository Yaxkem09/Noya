package com.example.noya.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.example.noya.MainActivity
import com.example.noya.data.getContactInfoFromNumber
import com.example.noya.ui.ResponsiveDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun IncomingCallScreen(
    callerNumber: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCallGone: () -> Unit
) {
    val context = LocalContext.current
    var callerName by remember { mutableStateOf<String?>(null) }
    var callerPhoto by remember { mutableStateOf<String?>(null) }
    var hasEnded by remember { mutableStateOf(false) }
    val callState by CallManager.callState.collectAsStateWithLifecycle()

    // Buscar el nombre y foto del contacto (en IO thread)
    LaunchedEffect(callerNumber) {
        val (name, photoUri) = withContext(Dispatchers.IO) {
            getContactInfoFromNumber(context, callerNumber)
        }
        callerName = name
        callerPhoto = photoUri
    }

    // Monitorear si la llamada que suena fue cancelada via Flow.
    // Se revisa la llamada que suena (no el estado general) porque puede
    // haber otra llamada activa al mismo tiempo (llamada en espera).
    LaunchedEffect(callState) {
        Log.d("IncomingCallScreen", "Estado de llamada actualizado: $callState")
        if (!hasEnded && CallManager.ringingCall() == null) {
            Log.d("IncomingCallScreen", "Llamada cancelada/terminada (estado: $callState) - cerrando pantalla")
            hasEnded = true
            onCallGone()
        }
    }

    // Verificación inicial con reintentos: esperar a que se establezca la llamada
    LaunchedEffect(Unit) {
        var attempts = 0
        val maxAttempts = 5
        while (attempts < maxAttempts) {
            delay(500) // Esperar 500ms entre cada intento
            if (CallManager.ringingCall() != null || hasEnded) {
                break
            }
            attempts++
            Log.d("IncomingCallScreen", "Esperando llamada entrante... intento ${attempts}/$maxAttempts")
        }

        if (CallManager.ringingCall() == null && !hasEnded) {
            Log.d("IncomingCallScreen", "No hay llamada entrante después de ${maxAttempts} intentos - cerrando pantalla")
            hasEnded = true
            onCallGone()
        }
    }

    // Mantener la pantalla encendida y sobre lock screen
    DisposableEffect(Unit) {
        val activity = context as? MainActivity
        activity?.enableShowOverLockScreen()

        onDispose {
            Log.d("IncomingCallScreen", "Pantalla de llamada entrante destruida - limpiando recursos")
            // El estado de la ventana ahora se controla desde MainActivity/lifecycleScope
        }
    }

    // Dimensiones responsivas
    val screenPadding = ResponsiveDimens.current.screenPadding
    val smallPadding = ResponsiveDimens.current.smallPadding
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val largePadding = ResponsiveDimens.current.largePadding
    val callPhotoSize = ResponsiveDimens.current.callPhotoSize
    val largeCornerRadius = ResponsiveDimens.current.largeCornerRadius
    val largeTextSize = ResponsiveDimens.current.largeTextSize
    val mediumTextSize = ResponsiveDimens.current.mediumTextSize
    val titleTextSize = ResponsiveDimens.current.titleTextSize
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
            if (callerPhoto != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(callerPhoto)
                        .crossfade(true)
                        .size(200) // Limitar tamaño de decodificación
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Foto de ${callerName ?: callerNumber}",
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

        // Texto "Llamada entrante"
        Text(
            text = "Llamada entrante",
            fontSize = largeTextSize.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(largePadding))

        // Nombre del contacto si está disponible
        if (callerName != null) {
            Text(
                text = callerName!!,
                fontSize = (36 * scaleFactor).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(smallPadding))
        }

        // Número de teléfono
        Text(
            text = callerNumber,
            fontSize = if (callerName != null) mediumTextSize.sp else titleTextSize.sp,
            fontWeight = if (callerName != null) FontWeight.Normal else FontWeight.Bold,
            color = Color(0xFF5D6D7E),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Botón para aceptar
        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(largeButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF58D68D), // Verde
                contentColor = Color.White
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Aceptar",
                    modifier = Modifier.size(largeIconSize)
                )
                Spacer(modifier = Modifier.width(mediumPadding))
                Text(
                    text = "Aceptar",
                    fontSize = largeTextSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(largePadding))

        // Botón para rechazar
        Button(
            onClick = onReject,
            modifier = Modifier
                .fillMaxWidth()
                .height(largeButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEC7063), // Rojo suave
                contentColor = Color.White
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "Rechazar",
                    modifier = Modifier.size(largeIconSize)
                )
                Spacer(modifier = Modifier.width(mediumPadding))
                Text(
                    text = "Rechazar",
                    fontSize = largeTextSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height((48 * scaleFactor).dp))
    }
}
