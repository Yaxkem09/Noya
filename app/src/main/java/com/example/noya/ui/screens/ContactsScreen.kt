package com.example.noya.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.noya.AppSettings
import com.example.noya.NoyaViewModel
import com.example.noya.data.Contact
import com.example.noya.ui.ResponsiveDimens
import com.example.noya.ui.components.LargeAccessibleButton
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    onNavigateBack: () -> Unit,
    onCallStarted: (Contact) -> Unit,
    viewModel: NoyaViewModel = viewModel()
) {
    val context = LocalContext.current

    // Estados del ViewModel
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingContacts.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasContactsPermission.collectAsStateWithLifecycle()
    // Se negó el permiso: mostrar mensaje con botones en vez de pantalla en blanco
    var permissionDenied by remember { mutableStateOf(false) }
    // El permiso se pidió desde el botón "Dar permiso"
    var askedFromButton by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readContactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val callPhoneGranted = permissions[Manifest.permission.CALL_PHONE] ?: false

        if (readContactsGranted && callPhoneGranted) {
            permissionDenied = false
            viewModel.loadContacts(true)
        } else {
            permissionDenied = true
            if (askedFromButton) {
                // Si Android ya no muestra el diálogo ("no volver a preguntar"),
                // abrir los ajustes de la app para dar el permiso a mano
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Activa los permisos de Contactos y Teléfono", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("ContactsScreen", "Error abriendo ajustes de la app: ${e.message}", e)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val callGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (readGranted && callGranted) {
            viewModel.loadContacts(true)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    // Dimensiones responsivas
    val screenPadding = ResponsiveDimens.current.screenPadding
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val mediumTextSize = ResponsiveDimens.current.mediumTextSize

    // Estado para la página actual (navegación tipo libro)
    var currentPage by remember { mutableIntStateOf(0) }

    // Calcular altura de cada tarjeta de contacto
    val hidePhotos by AppSettings.hideContactPhotos
    val photoSize by AppSettings.contactPhotoSize
    val scaleFactor = ResponsiveDimens.current.scaleFactor
    val scaledPhotoSize = photoSize * scaleFactor
    val cardHeightValue = if (hidePhotos) (80 * scaleFactor) else (scaledPhotoSize + 25 * scaleFactor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPadding)
    ) {
        // Botón de regresar al menú principal
        val smallPadding = ResponsiveDimens.current.smallPadding
        val smallIconSize = ResponsiveDimens.current.smallIconSize
        val smallTextSize = ResponsiveDimens.current.smallTextSize
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(ResponsiveDimens.current.extraSmallButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF85929E),
                contentColor = Color.White
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    modifier = Modifier.size(smallIconSize)
                )
                Spacer(modifier = Modifier.width(smallPadding))
                Text(
                    text = "Regresar",
                    fontSize = smallTextSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(mediumPadding))

        // Contenido de contactos (sin título)
        if (hasPermission) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF58D68D)
                        )
                    }
                }
                contacts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay contactos",
                            fontSize = mediumTextSize.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    // Navegación tipo página de libro
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Espacio para botones de navegación e indicador de página
                        val availableHeight = this.maxHeight - 100.dp - (mediumTextSize * 1.5f + mediumPadding.value).dp
                        val spacingValue = mediumPadding.value
                        val itemTotalHeight = cardHeightValue + spacingValue

                        // Calcular cuántos contactos caben por página
                        val contactsPerPage = ((availableHeight.value) / itemTotalHeight).toInt().coerceAtLeast(1)
                        val totalPages = (contacts.size + contactsPerPage - 1) / contactsPerPage

                        // Resetear página si es necesario
                        LaunchedEffect(totalPages) {
                            if (currentPage >= totalPages && totalPages > 0) {
                                currentPage = totalPages - 1
                            }
                        }

                        // Calcular rango de contactos para la página actual
                        val startIndex = currentPage * contactsPerPage
                        val endIndex = minOf(startIndex + contactsPerPage, contacts.size)
                        val pageContacts = contacts.subList(startIndex, endIndex)

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Lista de contactos de la página actual
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(mediumPadding)
                            ) {
                                pageContacts.forEach { contact ->
                                    ContactItem(
                                        contact = contact,
                                        onCallClick = {
                                            onCallStarted(contact)
                                        }
                                    )
                                }
                            }

                            // Indicador de página
                            Text(
                                text = "Página ${currentPage + 1} de $totalPages",
                                fontSize = mediumTextSize.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = mediumPadding)
                            )

                            // Botones de navegación (adelante/atrás)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = mediumPadding),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón Atrás
                                Button(
                                    onClick = {
                                        if (currentPage > 0) {
                                            currentPage--
                                        }
                                    },
                                    enabled = currentPage > 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(90.dp)
                                        .padding(end = mediumPadding / 2),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3498DB),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFFBDC3C7),
                                        disabledContentColor = Color.White
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(ResponsiveDimens.current.cornerRadius)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Página anterior",
                                        modifier = Modifier.size(50.dp)
                                    )
                                }

                                // Botón Adelante
                                Button(
                                    onClick = {
                                        if (currentPage < totalPages - 1) {
                                            currentPage++
                                        }
                                    },
                                    enabled = currentPage < totalPages - 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(90.dp)
                                        .padding(start = mediumPadding / 2),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3498DB),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFFBDC3C7),
                                        disabledContentColor = Color.White
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(ResponsiveDimens.current.cornerRadius)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Página siguiente",
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (permissionDenied) {
            // Sin permiso: explicar y dar salida (antes la pantalla quedaba en blanco)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(ResponsiveDimens.current.largeIconSize * 2),
                    tint = Color(0xFF85929E)
                )
                Spacer(modifier = Modifier.height(mediumPadding))
                Text(
                    text = "Noya necesita permiso para ver los contactos y hacer llamadas",
                    fontSize = mediumTextSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(ResponsiveDimens.current.largePadding * 2))
                LargeAccessibleButton(
                    text = "Dar permiso",
                    icon = Icons.Filled.Settings,
                    onClick = {
                        askedFromButton = true
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.CALL_PHONE
                            )
                        )
                    },
                    backgroundColor = Color(0xFF58D68D)
                )
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, onCallClick: () -> Unit) {
    val context = LocalContext.current
    val hideNames by AppSettings.hideContactNames
    val hidePhotos by AppSettings.hideContactPhotos
    val photoSize by AppSettings.contactPhotoSize

    // Dimensiones responsivas
    val scaleFactor = ResponsiveDimens.current.scaleFactor
    val smallPadding = ResponsiveDimens.current.smallPadding
    val smallCornerRadius = ResponsiveDimens.current.smallCornerRadius
    val cornerRadius = ResponsiveDimens.current.cornerRadius
    val normalTextSize = ResponsiveDimens.current.normalTextSize
    val tinyTextSize = ResponsiveDimens.current.tinyTextSize

    // Calcular altura del card basada en el tamaño de la foto (escalado)
    val scaledPhotoSize = photoSize * scaleFactor
    // Cuando se ocultan las fotos, usar una altura fija que permita mostrar nombre y número
    val cardHeight = if (hidePhotos) (80 * scaleFactor).dp else (scaledPhotoSize + 25 * scaleFactor).dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(smallPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(smallCornerRadius)
        ) {
            // Foto del contacto - cuadrada con esquinas redondeadas (solo si no está oculta)
            if (!hidePhotos) {
                Box(
                    modifier = Modifier
                        .size(scaledPhotoSize.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(smallCornerRadius))
                        .background(Color(0xFFE8E8E8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (contact.photoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(contact.photoUri)
                                .crossfade(true)
                                .size(150) // Limitar tamaño de decodificación
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "Foto de ${contact.name}",
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
                            modifier = Modifier.size((scaledPhotoSize * 0.5f).dp),
                            tint = Color(0xFF85929E)
                        )
                    }
                }
            }

            // Información del contacto (solo si no está oculto)
            if (!hideNames) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = contact.name,
                        fontSize = normalTextSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.phoneNumber,
                        fontSize = tinyTextSize.sp,
                        color = Color(0xFF85929E)
                    )
                }
            } else {
                // Espacio flexible cuando se ocultan los nombres
                Spacer(modifier = Modifier.weight(1f))
            }

            // Botón de llamar - tamaño proporcional a la foto o fijo si se ocultan las fotos
            val buttonSize = if (hidePhotos) (60f * scaleFactor) else (scaledPhotoSize * 0.75f).coerceIn(60f * scaleFactor, 100f * scaleFactor)
            Button(
                onClick = onCallClick,
                modifier = Modifier.size(buttonSize.dp),
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
                    modifier = Modifier.size((buttonSize * 0.5f).dp)
                )
            }
        }
    }
}
