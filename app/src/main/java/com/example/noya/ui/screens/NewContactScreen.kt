package com.example.noya.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.noya.NoyaViewModel
import com.example.noya.data.SaveContactResult
import com.example.noya.data.saveContactWithPhoto
import com.example.noya.ui.ResponsiveDimens
import com.example.noya.ui.components.NumericKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewContactScreen(
    onNavigateBack: () -> Unit,
    viewModel: NoyaViewModel = viewModel()
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Dimensiones responsivas
    val smallPadding = ResponsiveDimens.current.smallPadding
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val smallButtonHeight = ResponsiveDimens.current.smallButtonHeight
    val extraSmallButtonHeight = ResponsiveDimens.current.extraSmallButtonHeight
    val mediumTextSize = ResponsiveDimens.current.mediumTextSize
    val smallTextSize = ResponsiveDimens.current.smallTextSize
    val largeTextSize = ResponsiveDimens.current.largeTextSize
    val normalTextSize = ResponsiveDimens.current.normalTextSize
    val mediumIconSize = ResponsiveDimens.current.mediumIconSize
    val scaleFactor = ResponsiveDimens.current.scaleFactor

    // Launcher para seleccionar foto
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Se necesita permiso para guardar contactos", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(smallPadding)
    ) {
        // Botón de regresar
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(smallButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF85929E), // Gris suave
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
                    modifier = Modifier.size(mediumIconSize)
                )
                Spacer(modifier = Modifier.width(smallPadding))
                Text(
                    text = "Regresar",
                    fontSize = smallTextSize.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(smallPadding))

        // Contenido con scroll
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(smallPadding)
        ) {
            item {
                // Título
                Text(
                    text = "Nuevo Contacto",
                    fontSize = mediumTextSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50) // Azul oscuro suave
                )
            }

            item {
                // Display del número de teléfono
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((70 * scaleFactor).dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(smallPadding),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = phoneNumber.ifEmpty { "Número de teléfono" },
                            fontSize = largeTextSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phoneNumber.isEmpty()) Color(0xFF85929E) else Color(0xFF2C3E50) // Gris suave o azul oscuro
                        )
                    }
                }
            }

            item {
                // Teclado numérico más pequeño
                NumericKeyboard(
                    onNumberClick = { number ->
                        phoneNumber += number
                    },
                    onBackspaceClick = {
                        if (phoneNumber.isNotEmpty()) {
                            phoneNumber = phoneNumber.dropLast(1)
                        }
                    },
                    customButtonHeight = extraSmallButtonHeight,
                    customTextSize = normalTextSize
                )
            }

            item {
                // Campo de texto para el nombre del contacto
                var textWatcherAdded by remember { mutableStateOf(false) }

                AndroidView(
                    factory = { context ->
                        android.widget.EditText(context).apply {
                            hint = "Nombre del contacto"
                            textSize = 24f * scaleFactor
                            setTextColor(android.graphics.Color.BLACK)
                            setHintTextColor(android.graphics.Color.GRAY)
                            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                            val padding = (32 * scaleFactor).toInt()
                            setPadding(padding, padding, padding, padding)
                            inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                                       android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                                        android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                                        android.view.inputmethod.EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION
                            privateImeOptions = "nm,com.google.android.inputmethod.latin.noMicrophoneKey,nm.noAccessoryAction,nm.noPersonalizedLearning"
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            maxLines = 1
                            setSingleLine(true)
                            setShowSoftInputOnFocus(true)

                            // Agregar el TextWatcher solo una vez en factory
                            addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    if (s.toString() != contactName) {
                                        contactName = s.toString()
                                    }
                                }
                                override fun afterTextChanged(s: android.text.Editable?) {}
                            })
                        }
                    },
                    update = { editText ->
                        // Solo actualizar si el texto es diferente
                        if (editText.text.toString() != contactName) {
                            val selection = editText.selectionStart
                            editText.setText(contactName)
                            // Restaurar la posición del cursor
                            editText.setSelection(minOf(selection, contactName.length))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((70 * scaleFactor).dp)
                )
            }

            item {
                // Sección para agregar foto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = smallPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(mediumPadding)
                ) {
                    // Vista previa de la foto o placeholder
                    Box(
                        modifier = Modifier
                            .size((80 * scaleFactor).dp)
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape((12 * scaleFactor).dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPhotoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedPhotoUri)
                                    .crossfade(true)
                                    .size(150)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "Foto del contacto",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape((12 * scaleFactor).dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Sin foto",
                                modifier = Modifier.size((40 * scaleFactor).dp),
                                tint = Color(0xFF85929E)
                            )
                        }
                    }

                    // Botón para seleccionar foto
                    Button(
                        onClick = {
                            photoPickerLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(extraSmallButtonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9B59B6),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (selectedPhotoUri != null) "Cambiar Foto" else "Agregar Foto",
                            fontSize = smallTextSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                // Botón de guardar
                Button(
                    onClick = {
                        if (phoneNumber.isNotEmpty() && contactName.isNotEmpty()) {
                            if (hasPermission) {
                                if (isSaving) return@Button
                                isSaving = true
                                coroutineScope.launch {
                                    // Guardar fuera del hilo principal (la foto puede tardar)
                                    val result = withContext(Dispatchers.IO) {
                                        saveContactWithPhoto(context, contactName, phoneNumber, selectedPhotoUri)
                                    }
                                    isSaving = false
                                    when (result) {
                                        SaveContactResult.SAVED -> {
                                            Toast.makeText(context, "Contacto guardado", Toast.LENGTH_SHORT).show()
                                        }
                                        SaveContactResult.SAVED_WITHOUT_PHOTO -> {
                                            Toast.makeText(context, "Contacto guardado, pero no se pudo guardar la foto", Toast.LENGTH_LONG).show()
                                        }
                                        SaveContactResult.FAILED -> {
                                            Toast.makeText(context, "No se pudo guardar el contacto", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    if (result != SaveContactResult.FAILED) {
                                        viewModel.refreshContacts() // Actualizar lista de contactos
                                        onNavigateBack()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "No hay permiso para guardar contactos", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((65 * scaleFactor).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF58D68D), // Verde más suave y brillante
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Guardar",
                            modifier = Modifier.size(mediumIconSize)
                        )
                        Spacer(modifier = Modifier.width(smallPadding))
                        Text(
                            text = if (isSaving) "Guardando..." else "Guardar Contacto",
                            fontSize = smallTextSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                // Espacio extra al final para asegurar que el botón sea visible
                Spacer(modifier = Modifier.height(mediumPadding))
            }
        }
    }
}
