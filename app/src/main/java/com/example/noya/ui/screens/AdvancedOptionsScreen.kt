package com.example.noya.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.noya.AppSettings
import com.example.noya.NoyaViewModel
import com.example.noya.device.toggleSilentMode
import com.example.noya.ui.ResponsiveDimens
import com.example.noya.ui.components.LargeAccessibleButton

@Composable
fun AdvancedOptionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNewContact: () -> Unit,
    viewModel: NoyaViewModel = viewModel()
) {
    val context = LocalContext.current
    val isSilentMode by viewModel.isSilentMode.collectAsStateWithLifecycle()

    // Dimensiones responsivas
    val screenPadding = ResponsiveDimens.current.screenPadding
    val smallPadding = ResponsiveDimens.current.smallPadding
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val largePadding = ResponsiveDimens.current.largePadding
    val mediumIconSize = ResponsiveDimens.current.mediumIconSize
    val cornerRadius = ResponsiveDimens.current.cornerRadius
    val normalTextSize = ResponsiveDimens.current.normalTextSize
    val smallTextSize = ResponsiveDimens.current.smallTextSize
    val scaleFactor = ResponsiveDimens.current.scaleFactor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPadding)
    ) {
        // Botón de regresar
        val extraSmallButtonHeight = ResponsiveDimens.current.extraSmallButtonHeight
        val smallIconSize = ResponsiveDimens.current.smallIconSize
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(extraSmallButtonHeight),
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

        Spacer(modifier = Modifier.height(largePadding))

        // Botón Nuevo Contacto
        LargeAccessibleButton(
            text = "Nuevo Contacto",
            icon = Icons.Filled.Person,
            onClick = onNavigateToNewContact,
            backgroundColor = Color(0xFF58D68D), // Verde
            customHeight = extraSmallButtonHeight,
            customIconSize = smallIconSize,
            customTextSize = smallTextSize
        )

        Spacer(modifier = Modifier.height(largePadding))

        // Botón Silenciar / Activar sonido
        Button(
            onClick = {
                toggleSilentMode(context) {
                    viewModel.syncSilentMode()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(extraSmallButtonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSilentMode) Color(0xFFE67E22) else Color(0xFF5DADE2),
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (isSilentMode) "Activar sonido" else "Silenciar",
                fontSize = smallTextSize.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(largePadding))

        // Botón Configuraciones del Teléfono
        val smallButtonHeight = ResponsiveDimens.current.smallButtonHeight
        LargeAccessibleButton(
            text = "Configuraciones del Teléfono",
            icon = Icons.Filled.Build,
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir configuraciones", Toast.LENGTH_SHORT).show()
                    Log.e("AdvancedOptions", "Error abriendo configuraciones: ${e.message}", e)
                }
            },
            backgroundColor = Color(0xFF5DADE2), // Azul
            customHeight = smallButtonHeight,
            customIconSize = mediumIconSize,
            customTextSize = normalTextSize
        )

        Spacer(modifier = Modifier.height(largePadding))

        // Panel de Configuración
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = smallPadding),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
        ) {
            Column(
                modifier = Modifier.padding(largePadding)
            ) {
                Spacer(modifier = Modifier.height(mediumPadding))

                // Checkbox para ocultar nombres
                var hideNames by AppSettings.hideContactNames
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = smallPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ocultar nombres de contactos",
                        fontSize = smallTextSize.sp,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = hideNames,
                        onCheckedChange = { hideNames = it; AppSettings.save() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF58D68D),
                            uncheckedColor = Color(0xFF85929E)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(mediumPadding))

                // Checkbox para ocultar fotos
                var hidePhotos by AppSettings.hideContactPhotos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = smallPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ocultar fotos de contactos",
                        fontSize = smallTextSize.sp,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = hidePhotos,
                        onCheckedChange = { hidePhotos = it; AppSettings.save() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF58D68D),
                            uncheckedColor = Color(0xFF85929E)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(largePadding))

                // Slider para tamaño de fotos
                var photoSize by AppSettings.contactPhotoSize
                Text(
                    text = "Tamaño de fotos: ${photoSize.toInt()} dp",
                    fontSize = smallTextSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(mediumPadding))
                Slider(
                    value = photoSize,
                    onValueChange = { photoSize = it; AppSettings.save() },
                    valueRange = 60f..200f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF58D68D),
                        activeTrackColor = Color(0xFF58D68D),
                        inactiveTrackColor = Color(0xFFE8E8E8)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pequeño",
                        fontSize = (12 * scaleFactor).sp,
                        color = Color(0xFF85929E)
                    )
                    Text(
                        text = "Grande",
                        fontSize = (12 * scaleFactor).sp,
                        color = Color(0xFF85929E)
                    )
                }

                Spacer(modifier = Modifier.height(largePadding))

                // Slider para brillo de pantalla
                var currentBrightness by remember {
                    mutableFloatStateOf(
                        try {
                            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS).toFloat()
                        } catch (e: Exception) {
                            128f
                        }
                    )
                }

                // Permiso para cambiar el brillo (se vuelve a revisar al regresar de Ajustes)
                var canWriteSettings by remember { mutableStateOf(Settings.System.canWrite(context)) }
                var manualModeSet by remember { mutableStateOf(false) }
                LifecycleResumeEffect(Unit) {
                    canWriteSettings = Settings.System.canWrite(context)
                    onPauseOrDispose { }
                }

                if (!canWriteSettings) {
                    // Sin permiso: un botón que abre Ajustes una sola vez
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("AdvancedOptions", "Error abriendo permiso de brillo: ${e.message}", e)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(extraSmallButtonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF39C12),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Dar permiso para cambiar brillo",
                            fontSize = smallTextSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Brillo de pantalla: ${(currentBrightness / 255 * 100).toInt()}%",
                        fontSize = smallTextSize.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(mediumPadding))
                    Slider(
                        value = currentBrightness,
                        onValueChange = { newBrightness ->
                            currentBrightness = newBrightness
                            try {
                                // Poner el brillo en modo manual solo una vez
                                if (!manualModeSet) {
                                    Settings.System.putInt(
                                        context.contentResolver,
                                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                                    )
                                    manualModeSet = true
                                }
                                Settings.System.putInt(
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    newBrightness.toInt()
                                )
                            } catch (e: Exception) {
                                Log.e("AdvancedOptions", "Error al cambiar brillo: ${e.message}", e)
                            }
                        },
                        valueRange = 1f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF39C12),
                            activeTrackColor = Color(0xFFF39C12),
                            inactiveTrackColor = Color(0xFFE8E8E8)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Oscuro",
                            fontSize = (12 * scaleFactor).sp,
                            color = Color(0xFF85929E)
                        )
                        Text(
                            text = "Brillante",
                            fontSize = (12 * scaleFactor).sp,
                            color = Color(0xFF85929E)
                        )
                    }
                }
            }
        }
    }
}
