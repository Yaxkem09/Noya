package com.example.noya.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.noya.AppSettings
import com.example.noya.NoyaViewModel
import com.example.noya.R
import com.example.noya.data.CallLog
import com.example.noya.data.Contact
import com.example.noya.device.toggleSilentMode
import com.example.noya.ui.ResponsiveDimens
import com.example.noya.ui.components.GridImageButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToContacts: () -> Unit,
    onNavigateToAdvancedOptions: () -> Unit,
    onCallContact: (Contact) -> Unit,
    viewModel: NoyaViewModel = viewModel()
) {
    val context = LocalContext.current

    // Estados del ViewModel usando collectAsState
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val batteryStatus by viewModel.batteryStatus.collectAsStateWithLifecycle()
    val isSilentMode by viewModel.isSilentMode.collectAsStateWithLifecycle()
    val missedCalls by viewModel.missedCalls.collectAsStateWithLifecycle()

    // Estados locales de UI
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var isPressing by remember { mutableStateOf(false) }

    val missedCallsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Solo se piden los permisos que faltan: los que ya estaban dados no vienen en el resultado
        fun granted(permission: String) = permissions[permission]
            ?: (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
        val readGranted = granted(Manifest.permission.READ_CALL_LOG)
        val callGranted = granted(Manifest.permission.CALL_PHONE)

        if (readGranted) {
            viewModel.loadMissedCalls(true)
            viewModel.startMissedCallsUpdates()
        } else {
            Toast.makeText(context, "Se necesitan permisos para mostrar llamadas perdidas", Toast.LENGTH_LONG).show()
        }

        if (!callGranted) {
            Toast.makeText(context, "Concede permiso de llamadas para devolver una llamada fácilmente", Toast.LENGTH_LONG).show()
        }
    }

    // Cargar llamadas perdidas al iniciar y pedir los permisos que falten
    // (incluye notificaciones, para la notificación de llamada entrante)
    LaunchedEffect(Unit) {
        fun isGranted(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        val neededPermissions = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missingPermissions = neededPermissions.filterNot { isGranted(it) }

        if (isGranted(Manifest.permission.READ_CALL_LOG) && isGranted(Manifest.permission.CALL_PHONE)) {
            viewModel.loadMissedCalls(true)
            viewModel.startMissedCallsUpdates()
        }
        if (missingPermissions.isNotEmpty()) {
            missedCallsPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // Manejar el progreso de la presión
    LaunchedEffect(isPressing) {
        if (isPressing) {
            val startTime = System.currentTimeMillis()
            while (isPressing) {
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed / 3000f).coerceIn(0f, 1f)

                if (pressProgress >= 1f) {
                    // Activar opciones avanzadas
                    isPressing = false
                    pressProgress = 0f
                    onNavigateToAdvancedOptions()
                    break
                }
                kotlinx.coroutines.delay(16) // ~60fps
            }
        } else {
            // Resetear el progreso cuando se suelta
            pressProgress = 0f
        }
    }

    // Obtener dimensiones responsivas
    val screenPadding = ResponsiveDimens.current.screenPadding
    val smallPadding = ResponsiveDimens.current.smallPadding
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val largePadding = ResponsiveDimens.current.largePadding
    val timeTextSize = ResponsiveDimens.current.timeTextSize
    val dateTextSize = ResponsiveDimens.current.dateTextSize
    val smallTextSize = ResponsiveDimens.current.smallTextSize
    val mediumIconSize = ResponsiveDimens.current.mediumIconSize
    val smallIconSize = ResponsiveDimens.current.smallIconSize
    val settingsButtonSize = ResponsiveDimens.current.settingsButtonSize

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hora y Fecha en la parte superior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (60 * ResponsiveDimens.current.scaleFactor).dp, bottom = largePadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime,
                    fontSize = timeTextSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50), // Azul oscuro suave
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(smallPadding))
                Text(
                    text = currentDate,
                    fontSize = dateTextSize.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF5D6D7E), // Gris azulado
                    textAlign = TextAlign.Center
                )
            }

        Spacer(modifier = Modifier.height(mediumPadding))

        // Botones principales - solo Llamar y Silenciar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(mediumPadding)
        ) {
            // Botón Llamar
            GridImageButton(
                text = "Llamar",
                imageRes = R.drawable.ic_btn_llamar,
                onClick = { onNavigateToContacts() },
                modifier = Modifier.weight(1f)
            )

            // Botón Modo Silencio
            GridImageButton(
                text = if (isSilentMode) "Sonido" else "Silenciar",
                imageRes = if (isSilentMode) R.drawable.ic_btn_sonido else R.drawable.ic_btn_silenciar,
                onClick = {
                    toggleSilentMode(context) {
                        viewModel.syncSilentMode()
                    }
                },
                backgroundColor = if (isSilentMode) Color(0xFFE67E22) else Color(0xFF5DADE2),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(mediumPadding))

        // Panel de Llamadas Perdidas
        MissedCallsPanel(
            missedCalls = missedCalls,
            onCallContact = onCallContact,
            modifier = Modifier.weight(1f)
        )
        }

        // Botón de opciones avanzadas en la esquina superior izquierda
        Box(
            modifier = Modifier
                .padding(top = smallPadding, start = smallPadding)
                .size(settingsButtonSize)
                .align(Alignment.TopStart)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressing = true
                            tryAwaitRelease()
                            isPressing = false
                        }
                    )
                }
        ) {
            // Indicador de progreso circular
            if (pressProgress > 0f) {
                CircularProgressIndicator(
                    progress = { pressProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF58D68D),
                    strokeWidth = 4.dp
                )
            }

            // Botón
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Opciones Avanzadas",
                    modifier = Modifier.size(mediumIconSize),
                    tint = Color(0xFF2C3E50)
                )
            }
        }

        // Indicador de batería en la esquina superior derecha
        Box(
            modifier = Modifier
                .padding(mediumPadding)
                .align(Alignment.TopEnd)
        ) {
            // Color según el nivel de batería
            val batteryColor = when {
                batteryStatus.level > 80 -> Color(0xFF27AE60) // Verde brillante
                batteryStatus.level > 50 -> Color(0xFF58D68D) // Verde claro
                batteryStatus.level > 30 -> Color(0xFFF39C12) // Amarillo/Naranja
                batteryStatus.level > 15 -> Color(0xFFE67E22) // Naranja
                else -> Color(0xFFE74C3C) // Rojo
            }

            // Icono según el nivel de batería
            val batteryIcon = when {
                batteryStatus.isCharging -> Icons.Filled.BatteryChargingFull
                batteryStatus.level > 85 -> Icons.Filled.BatteryFull
                batteryStatus.level > 70 -> Icons.Filled.Battery6Bar
                batteryStatus.level > 50 -> Icons.Filled.Battery5Bar
                batteryStatus.level > 30 -> Icons.Filled.Battery4Bar
                batteryStatus.level > 15 -> Icons.Filled.Battery2Bar
                else -> Icons.Filled.Battery0Bar
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = if (batteryStatus.isCharging) {
                    Modifier
                        .background(
                            color = Color(0xFF27AE60).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    Modifier
                }
            ) {
                // Icono de rayo si está cargando
                if (batteryStatus.isCharging) {
                    Icon(
                        imageVector = Icons.Filled.ElectricBolt,
                        contentDescription = "Cargando",
                        modifier = Modifier.size(smallIconSize * 0.7f),
                        tint = Color(0xFF27AE60)
                    )
                }

                Icon(
                    imageVector = batteryIcon,
                    contentDescription = if (batteryStatus.isCharging) "Batería cargando" else "Batería",
                    modifier = Modifier.size(smallIconSize),
                    tint = if (batteryStatus.isCharging) Color(0xFF27AE60) else batteryColor
                )
                Text(
                    text = "${batteryStatus.level}%",
                    fontSize = smallTextSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (batteryStatus.isCharging) Color(0xFF27AE60) else batteryColor
                )
            }
        }
    }
}

@Composable
fun MissedCallsPanel(
    missedCalls: List<CallLog>,
    onCallContact: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dimensiones responsivas
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val smallPadding = ResponsiveDimens.current.smallPadding
    val cornerRadius = ResponsiveDimens.current.cornerRadius
    val smallIconSize = ResponsiveDimens.current.smallIconSize
    val normalTextSize = ResponsiveDimens.current.normalTextSize
    val smallTextSize = ResponsiveDimens.current.smallTextSize

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
            .background(Color(0xFFB71C1C).copy(alpha = 0.65f)) // Rojo semi-transparente
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(mediumPadding)
        ) {
            // Título del panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallMissed,
                    contentDescription = "Llamadas Perdidas",
                    modifier = Modifier.size(smallIconSize),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(smallPadding))
                Text(
                    text = "Llamadas Perdidas",
                    fontSize = normalTextSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(smallPadding))

            if (missedCalls.isEmpty()) {
                // Mensaje cuando no hay llamadas perdidas
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay llamadas perdidas",
                        fontSize = smallTextSize.sp,
                        color = Color(0xFFFFCDD2), // Rojo claro
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista de llamadas perdidas
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(smallPadding)
                ) {
                    items(missedCalls, key = { it.id }) { callLog ->
                        MissedCallPanelItem(
                            callLog = callLog,
                            onCallClick = {
                                val contact = Contact(
                                    callLog.id,
                                    callLog.name ?: callLog.phoneNumber,
                                    callLog.phoneNumber
                                )
                                onCallContact(contact)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissedCallPanelItem(
    callLog: CallLog,
    onCallClick: () -> Unit
) {
    val context = LocalContext.current
    val hideNames by AppSettings.hideContactNames
    val hidePhotos by AppSettings.hideContactPhotos
    val photoSize by AppSettings.contactPhotoSize

    // Dimensiones responsivas
    val scaleFactor = ResponsiveDimens.current.scaleFactor
    val smallPadding = ResponsiveDimens.current.smallPadding
    val smallCornerRadius = ResponsiveDimens.current.smallCornerRadius
    val cornerRadius = ResponsiveDimens.current.cornerRadius
    val mediumTextSize = ResponsiveDimens.current.mediumTextSize
    val tinyTextSize = ResponsiveDimens.current.tinyTextSize
    val mediumIconSize = ResponsiveDimens.current.mediumIconSize

    // Tamaño de foto para el panel (escalado)
    val panelPhotoSize = (photoSize * 0.8f * scaleFactor).coerceIn(70f * scaleFactor, 120f * scaleFactor)

    // Tarjeta contenedora
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFB71C1C) // Rojo oscuro para la tarjeta
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(smallPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(smallCornerRadius)
        ) {
            // Foto del contacto (solo si no está oculta)
            if (!hidePhotos) {
                Box(
                    modifier = Modifier
                        .size(panelPhotoSize.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(smallCornerRadius))
                        .background(Color(0xFFFFCDD2)),
                    contentAlignment = Alignment.Center
                ) {
                    if (callLog.photoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(callLog.photoUri)
                                .crossfade(true)
                                .size(100) // Limitar tamaño de decodificación
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "Foto de ${callLog.name ?: callLog.phoneNumber}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(smallCornerRadius)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Icono por defecto si no tiene foto
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Sin foto",
                            modifier = Modifier.size((panelPhotoSize * 0.5f).dp),
                            tint = Color(0xFFB71C1C)
                        )
                    }
                }
            }

            // Información de la llamada (solo si no está oculto)
            if (!hideNames) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = callLog.name ?: callLog.phoneNumber,
                        fontSize = mediumTextSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = callLog.time,
                        fontSize = tinyTextSize.sp,
                        color = Color(0xFFFFCDD2) // Rojo claro para la hora
                    )
                }
            } else {
                // Espacio flexible cuando se ocultan los nombres
                Spacer(modifier = Modifier.weight(1f))
            }

            // Botón de llamar
            val callButtonSize = (60 * scaleFactor).dp
            Button(
                onClick = onCallClick,
                modifier = Modifier.size(callButtonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58D68D),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(smallCornerRadius)
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Llamar",
                    modifier = Modifier.size(mediumIconSize)
                )
            }
        }
    }
}
