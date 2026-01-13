package com.example.noya

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.Call
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.noya.ui.theme.NoyaTheme
import java.text.SimpleDateFormat
import java.util.*

object IncomingCallState {
    var incomingCallNumber = mutableStateOf<String?>(null)
    var callEnded = mutableStateOf(false)
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_SET_DEFAULT_DIALER = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate llamado")

        // Verificar si hay una llamada entrante al crear
        handleIncomingCallIntent(intent)

        setContent {
            NoyaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5DC) // Color beige suave
                ) {
                    MainScreen()
                }
            }
        }

        // Solicitar ser la aplicación de teléfono predeterminada DESPUÉS de setContent
        requestDefaultDialerRole()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it)
            handleIncomingCallIntent(it)
        }
    }

    private fun handleIncomingCallIntent(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra("INCOMING_CALL", false)) {
                val callerNumber = it.getStringExtra("CALLER_NUMBER") ?: "Desconocido"
                Log.d("MainActivity", "Llamada entrante detectada: $callerNumber")
                IncomingCallState.incomingCallNumber.value = callerNumber
                // Limpiar el intent
                it.removeExtra("INCOMING_CALL")
                it.removeExtra("CALLER_NUMBER")
            }
        }
    }

    private fun requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 y superior - usar RoleManager
            val roleManager = getSystemService(RoleManager::class.java)
            val isDefaultDialer = roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            Log.d("MainActivity", "¿Es app de marcación predeterminada? $isDefaultDialer")

            if (!isDefaultDialer) {
                Log.d("MainActivity", "Solicitando permisos de marcación predeterminada")
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
            } else {
                Log.d("MainActivity", "Ya somos la app de marcación predeterminada")
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6 a Android 9 - usar TelecomManager
            val telecomManager = getSystemService(TelecomManager::class.java)
            val isDefaultDialer = telecomManager.defaultDialerPackage == packageName
            Log.d("MainActivity", "¿Es app de marcación predeterminada? $isDefaultDialer")

            if (!isDefaultDialer) {
                Log.d("MainActivity", "Solicitando permisos de marcación predeterminada")
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
                startActivity(intent)
            } else {
                Log.d("MainActivity", "Ya somos la app de marcación predeterminada")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    Log.d("MainActivity", "Usuario aceptó ser app predeterminada")
                    Toast.makeText(this, "Noya configurada como app de teléfono", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("MainActivity", "Usuario rechazó ser app predeterminada")
                    Toast.makeText(this, "Noya necesita ser la app de teléfono predeterminada para funcionar", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String
)

data class CallLog(
    val id: String,
    val phoneNumber: String,
    val name: String?,
    val date: String,
    val time: String
)

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("home") }
    var activeCallContact by remember { mutableStateOf<Contact?>(null) }
    val incomingCallerNumber by IncomingCallState.incomingCallNumber
    val callEnded by IncomingCallState.callEnded

    // Observar cambios en el número de llamada entrante
    LaunchedEffect(incomingCallerNumber) {
        if (incomingCallerNumber != null) {
            Log.d("MainScreen", "Mostrando pantalla de llamada entrante para: $incomingCallerNumber")
            currentScreen = "incomingCall"
        }
    }

    // Observar cuando la llamada termina
    LaunchedEffect(callEnded) {
        if (callEnded) {
            Log.d("MainScreen", "Llamada terminada, regresando a home")
            currentScreen = "home"
            activeCallContact = null
            IncomingCallState.callEnded.value = false
        }
    }

    when (currentScreen) {
        "home" -> HomeScreen(
            onNavigateToContacts = { currentScreen = "contacts" },
            onNavigateToAdvancedOptions = { currentScreen = "advancedOptions" },
            onNavigateToMissedCalls = { currentScreen = "missedCalls" }
        )
        "contacts" -> ContactsScreen(
            onNavigateBack = { currentScreen = "home" },
            onCallStarted = { contact ->
                activeCallContact = contact
                currentScreen = "activeCall"
            }
        )
        "newContact" -> NewContactScreen(
            onNavigateBack = { currentScreen = "advancedOptions" }
        )
        "advancedOptions" -> AdvancedOptionsScreen(
            onNavigateBack = { currentScreen = "home" },
            onNavigateToNewContact = { currentScreen = "newContact" }
        )
        "missedCalls" -> MissedCallsScreen(
            onNavigateBack = { currentScreen = "home" },
            onCallContact = { contact ->
                activeCallContact = contact
                currentScreen = "activeCall"
            }
        )
        "incomingCall" -> IncomingCallScreen(
            callerNumber = incomingCallerNumber ?: "Desconocido",
            onAccept = {
                CallManager.answerCall()
                val contact = Contact("incoming", incomingCallerNumber ?: "Desconocido", incomingCallerNumber ?: "")
                activeCallContact = contact
                IncomingCallState.incomingCallNumber.value = null
                currentScreen = "activeCall"
            },
            onReject = {
                CallManager.rejectCall()
                IncomingCallState.incomingCallNumber.value = null
                currentScreen = "home"
            }
        )
        "activeCall" -> activeCallContact?.let { contact ->
            ActiveCallScreen(
                contact = contact,
                onEndCall = { currentScreen = "home" }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateToContacts: () -> Unit,
    onNavigateToAdvancedOptions: () -> Unit,
    onNavigateToMissedCalls: () -> Unit
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var currentDate by remember { mutableStateOf(getCurrentDate()) }
    var isSilentMode by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableStateOf(0f) }
    var isPressing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Actualizar la hora cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            currentDate = getCurrentDate()
            kotlinx.coroutines.delay(1000)
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hora y Fecha en la parte superior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50), // Azul oscuro suave
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDate,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF5D6D7E), // Gris azulado
                    textAlign = TextAlign.Center
                )
            }

        Spacer(modifier = Modifier.weight(1f))

        // Botones principales
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón Llamadas
            LargeAccessibleButton(
                text = "Llamar5",
                icon = Icons.Filled.Call,
                onClick = {
                    onNavigateToContacts()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Llamadas Perdidas
            LargeAccessibleButton(
                text = "Llamadas Perdidas",
                icon = Icons.Filled.CallMissed,
                onClick = {
                    onNavigateToMissedCalls()
                },
                backgroundColor = Color(0xFFE74C3C) // Rojo suave
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Modo Silencio
            LargeAccessibleButton(
                text = if (isSilentMode) "Activar Sonido" else "Silenciar",
                icon = if (isSilentMode) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                onClick = {
                    toggleSilentMode(context) { newMode ->
                        isSilentMode = newMode
                    }
                },
                backgroundColor = if (isSilentMode) Color(0xFFE67E22) else Color(0xFF5DADE2) // Naranja cuando está silenciado, azul cuando está normal
            )
        }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Botón de opciones avanzadas en la esquina superior izquierda
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(60.dp)
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
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF2C3E50)
                )
            }
        }
    }
}

@Composable
fun ContactsScreen(onNavigateBack: () -> Unit, onCallStarted: (Contact) -> Unit) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readContactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val callPhoneGranted = permissions[Manifest.permission.CALL_PHONE] ?: false

        if (readContactsGranted && callPhoneGranted) {
            hasPermission = true
            contacts = getContacts(context)
        } else {
            Toast.makeText(context, "Se necesitan permisos para ver contactos y realizar llamadas", Toast.LENGTH_LONG).show()
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
            hasPermission = true
            contacts = getContacts(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Botón de regresar
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
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
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Regresar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Título
        Text(
            text = "Contactos",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50), // Azul oscuro suave
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Lista de contactos
        if (hasPermission) {
            if (contacts.isEmpty()) {
                Text(
                    text = "No hay contactos",
                    fontSize = 24.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(contacts) { contact ->
                        ContactItem(
                            contact = contact,
                            onCallClick = {
                                onCallStarted(contact)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, onCallClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Información del contacto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phoneNumber,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
            }

            // Botón de llamar
            Button(
                onClick = onCallClick,
                modifier = Modifier
                    .size(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58D68D), // Verde más suave y brillante
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Llamar0",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

fun getContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val cursor: Cursor? = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)

        while (it.moveToNext()) {
            val id = it.getString(idIndex)
            val name = it.getString(nameIndex) ?: "Sin nombre"
            val number = it.getString(numberIndex) ?: ""

            if (number.isNotEmpty()) {
                contacts.add(Contact(id, name, number))
            }
        }
    }

    return contacts
}

fun makePhoneCall(context: Context, phoneNumber: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val uri = Uri.fromParts("tel", phoneNumber, null)

            // Verificar que somos la app de marcación predeterminada
            if (telecomManager.defaultDialerPackage == context.packageName) {
                // Usar TelecomManager para realizar la llamada
                val bundle = Bundle()
                telecomManager.placeCall(uri, bundle)
            } else {
                // Si no somos la app predeterminada, solicitar que nos configuren
                Toast.makeText(context, "Por favor, configura Noya como app de teléfono predeterminada", Toast.LENGTH_LONG).show()
            }
        } else {
            // Para versiones anteriores a Android 6.0
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "No se puede realizar la llamada. Verifica los permisos.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al realizar la llamada: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun openWhatsApp(context: Context) {
    try {
        // Intentar abrir WhatsApp
        val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            // Si WhatsApp no está instalado, intentar con WhatsApp Business
            val businessIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
            if (businessIntent != null) {
                businessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(businessIntent)
            } else {
                Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error al abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ActiveCallScreen(contact: Contact, onEndCall: () -> Unit) {
    val context = LocalContext.current
    var isSpeakerOn by remember { mutableStateOf(false) }
    var callState by remember { mutableStateOf<Int?>(null) }
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var hasEnded by remember { mutableStateOf(false) }

    // Monitorear el estado de la llamada con callback
    LaunchedEffect(Unit) {
        CallManager.setCallStateCallback { state ->
            callState = state
            Log.d("ActiveCallScreen", "Estado de llamada actualizado: $state")

            // Si la llamada se desconectó, cerrar la pantalla
            if (state == Call.STATE_DISCONNECTED && !hasEnded) {
                Log.d("ActiveCallScreen", "Llamada desconectada - cerrando pantalla")
                hasEnded = true
                audioManager.isSpeakerphoneOn = false
                onEndCall()
            }
        }
    }

    // Verificar periódicamente si la llamada sigue activa
    LaunchedEffect(Unit) {
        while (!hasEnded) {
            kotlinx.coroutines.delay(500) // Verificar cada medio segundo

            val currentCall = CallManager.ongoingCall
            if (currentCall == null && !hasEnded) {
                Log.d("ActiveCallScreen", "Llamada ya no existe - cerrando pantalla")
                hasEnded = true
                audioManager.isSpeakerphoneOn = false
                onEndCall()
                return@LaunchedEffect
            }

            // Verificar el estado de la llamada directamente
            currentCall?.let { call ->
                if (call.state == Call.STATE_DISCONNECTED && !hasEnded) {
                    Log.d("ActiveCallScreen", "Llamada desconectada (verificación periódica) - cerrando pantalla")
                    hasEnded = true
                    audioManager.isSpeakerphoneOn = false
                    onEndCall()
                    return@LaunchedEffect
                }
            }
        }
    }

    // Iniciar la llamada cuando se muestra la pantalla (solo para llamadas salientes)
    LaunchedEffect(contact.id) {
        // Solo iniciar llamada si no hay una llamada en curso
        if (CallManager.ongoingCall == null && contact.id != "incoming") {
            makePhoneCall(context, contact.phoneNumber)

            // Mantener trayendo nuestra actividad al frente cada 300ms durante los primeros 3 segundos
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            repeat(10) { iteration ->
                handler.postDelayed({
                    val bringToFrontIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(bringToFrontIntent)
                }, (iteration * 300).toLong())
            }
        }
    }

    // Mantener la pantalla encendida y limpiar al salir
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            Log.d("ActiveCallScreen", "Pantalla de llamada destruida - limpiando recursos")
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            audioManager.isSpeakerphoneOn = false
            // Limpiar el callback para evitar memory leaks
            CallManager.setCallStateCallback { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Icono de llamada
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = "En llamada",
            modifier = Modifier.size(120.dp),
            tint = Color(0xFF58D68D) // Verde más suave
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Nombre del contacto
        Text(
            text = contact.name,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50), // Azul oscuro suave
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Número de teléfono
        Text(
            text = contact.phoneNumber,
            fontSize = 24.sp,
            color = Color(0xFF5D6D7E), // Gris azulado
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Estado de llamada
        Text(
            text = "En llamada...",
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF85929E), // Gris suave
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Botón de altavoz
        Button(
            onClick = {
                isSpeakerOn = !isSpeakerOn
                audioManager.isSpeakerphoneOn = isSpeakerOn
                Toast.makeText(
                    context,
                    if (isSpeakerOn) "Altavoz activado" else "Altavoz desactivado",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
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
                    imageVector = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = "Altavoz",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isSpeakerOn) "Desactivar Altavoz" else "Activar Altavoz",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para colgar
        Button(
            onClick = {
                audioManager.isSpeakerphoneOn = false
                CallManager.endCall()
                onEndCall()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
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
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Colgar",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Colgar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun IncomingCallScreen(
    callerNumber: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current
    var callerName by remember { mutableStateOf<String?>(null) }

    // Buscar el nombre del contacto
    LaunchedEffect(callerNumber) {
        callerName = getContactNameFromNumber(context, callerNumber)
    }

    // Mantener la pantalla encendida
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Icono de llamada entrante
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = "Llamada entrante",
            modifier = Modifier.size(120.dp),
            tint = Color(0xFF58D68D)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Texto "Llamada entrante"
        Text(
            text = "Llamada entrante",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre del contacto si está disponible
        if (callerName != null) {
            Text(
                text = callerName!!,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Número de teléfono
        Text(
            text = callerNumber,
            fontSize = if (callerName != null) 24.sp else 32.sp,
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
                .height(100.dp),
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
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Aceptar",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para rechazar
        Button(
            onClick = onReject,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
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
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Rechazar",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Rechazar",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun NewContactScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }

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
            .padding(12.dp)
    ) {
        // Botón de regresar
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Regresar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contenido con scroll
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Título
                Text(
                    text = "Nuevo Contacto",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50) // Azul oscuro suave
                )
            }

            item {
                // Display del número de teléfono
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (phoneNumber.isEmpty()) "Número de teléfono" else phoneNumber,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phoneNumber.isEmpty()) Color(0xFF85929E) else Color(0xFF2C3E50) // Gris suave o azul oscuro
                        )
                    }
                }
            }

            item {
                // Teclado numérico
                NumericKeyboard(
                    onNumberClick = { number ->
                        phoneNumber += number
                    },
                    onBackspaceClick = {
                        if (phoneNumber.isNotEmpty()) {
                            phoneNumber = phoneNumber.dropLast(1)
                        }
                    }
                )
            }

            item {
                // Campo de texto para el nombre del contacto
                var textWatcherAdded by remember { mutableStateOf(false) }

                AndroidView(
                    factory = { context ->
                        android.widget.EditText(context).apply {
                            hint = "Nombre del contacto"
                            textSize = 24f
                            setTextColor(android.graphics.Color.BLACK)
                            setHintTextColor(android.graphics.Color.GRAY)
                            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                            setPadding(32, 32, 32, 32)
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
                        .height(70.dp)
                )
            }

            item {
                // Botón de guardar
                Button(
                    onClick = {
                        if (phoneNumber.isNotEmpty() && contactName.isNotEmpty()) {
                            if (hasPermission) {
                                saveContact(context, contactName, phoneNumber)
                                Toast.makeText(context, "Contacto guardado", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "No hay permiso para guardar contactos", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Guardar Contacto",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                // Espacio extra al final para asegurar que el botón sea visible
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { digit ->
                    Button(
                        onClick = { onNumberClick(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34495E), // Azul grisáceo oscuro pero suave
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = digit,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Fila para el botón de borrar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onBackspaceClick,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE67E22), // Naranja más suave
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Backspace,
                    contentDescription = "Borrar",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

fun saveContact(context: Context, name: String, phoneNumber: String) {
    try {
        // Intentar guardar en la tarjeta SIM primero
        val simUri = Uri.parse("content://icc/adn")

        val simValues = android.content.ContentValues().apply {
            put("tag", name) // nombre del contacto en SIM
            put("number", phoneNumber) // número de teléfono en SIM
        }

        try {
            val result = context.contentResolver.insert(simUri, simValues)
            if (result != null) {
                Toast.makeText(context, "Contacto guardado en SIM", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            // Si falla guardar en SIM, intentar en el teléfono
            Log.e("SaveContact", "Error guardando en SIM: ${e.message}")
        }

        // Si no se pudo guardar en SIM, guardar en el teléfono
        val contentValues = android.content.ContentValues().apply {
            put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
            put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
        }

        val rawContactUri = context.contentResolver.insert(
            ContactsContract.RawContacts.CONTENT_URI,
            contentValues
        )

        val rawContactId = rawContactUri?.lastPathSegment?.toLong() ?: return

        // Insertar nombre
        val nameValues = android.content.ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

        // Insertar número de teléfono
        val phoneValues = android.content.ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
            put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)

        Toast.makeText(context, "Contacto guardado en teléfono", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error guardando contacto: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e("SaveContact", "Error: ${e.message}", e)
    }
}

fun getContactNameFromNumber(context: Context, phoneNumber: String): String? {
    try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return it.getString(nameIndex)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("GetContactName", "Error: ${e.message}", e)
    }
    return null
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
    return sdf.format(Date())
}

fun toggleSilentMode(context: Context, onModeChanged: (Boolean) -> Unit) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // Solicitar permiso para modificar el modo No Molestar
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            context.startActivity(intent)
            Toast.makeText(context, "Por favor, concede permiso para controlar el sonido", Toast.LENGTH_LONG).show()
            return
        }
    }

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            onModeChanged(true)
            Toast.makeText(context, "Modo silencio activado", Toast.LENGTH_SHORT).show()
        }
        else -> {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            onModeChanged(false)
            Toast.makeText(context, "Sonido activado", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun LargeAccessibleButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color = Color(0xFF5DADE2) // Azul claro y suave por defecto
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

fun getMissedCalls(context: Context): List<CallLog> {
    val missedCalls = mutableListOf<CallLog>()

    try {
        val cursor = context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            null,
            "${android.provider.CallLog.Calls.TYPE} = ?",
            arrayOf(android.provider.CallLog.Calls.MISSED_TYPE.toString()),
            "${android.provider.CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val dateIndex = it.getColumnIndex(android.provider.CallLog.Calls.DATE)
            val idIndex = it.getColumnIndex(android.provider.CallLog.Calls._ID)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val number = it.getString(numberIndex) ?: "Desconocido"
                val dateMillis = it.getLong(dateIndex)

                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateMillis))
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(dateMillis))
                val name = getContactNameFromNumber(context, number)

                missedCalls.add(CallLog(id, number, name, date, time))
            }
        }
    } catch (e: Exception) {
        Log.e("MissedCalls", "Error obteniendo llamadas perdidas: ${e.message}", e)
    }

    return missedCalls
}

@Composable
fun MissedCallsScreen(
    onNavigateBack: () -> Unit,
    onCallContact: (Contact) -> Unit
) {
    val context = LocalContext.current
    var missedCalls by remember { mutableStateOf<List<CallLog>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readCallLogGranted = permissions[Manifest.permission.READ_CALL_LOG] ?: false
        val callPhoneGranted = permissions[Manifest.permission.CALL_PHONE] ?: false

        if (readCallLogGranted && callPhoneGranted) {
            hasPermission = true
            missedCalls = getMissedCalls(context)
        } else {
            Toast.makeText(context, "Se necesitan permisos para ver llamadas perdidas", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val callGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (readGranted && callGranted) {
            hasPermission = true
            missedCalls = getMissedCalls(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Botón de regresar
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
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
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Regresar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Título
        Text(
            text = "Llamadas Perdidas",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Lista de llamadas perdidas
        if (hasPermission) {
            if (missedCalls.isEmpty()) {
                Text(
                    text = "No hay llamadas perdidas",
                    fontSize = 24.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(missedCalls) { callLog ->
                        MissedCallItem(
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
fun MissedCallItem(callLog: CallLog, onCallClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Información de la llamada
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = callLog.name ?: callLog.phoneNumber,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (callLog.name != null) {
                    Text(
                        text = callLog.phoneNumber,
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = "${callLog.date} - ${callLog.time}",
                    fontSize = 16.sp,
                    color = Color(0xFFE74C3C),
                    fontWeight = FontWeight.Medium
                )
            }

            // Botón de llamar
            Button(
                onClick = onCallClick,
                modifier = Modifier
                    .size(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58D68D),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Llamar",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun AdvancedOptionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNewContact: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Botón de regresar
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
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
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Regresar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Título
        Text(
            text = "Opciones Avanzadas",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Botón Nuevo Contacto
        LargeAccessibleButton(
            text = "Nuevo Contacto",
            icon = Icons.Filled.Person,
            onClick = onNavigateToNewContact,
            backgroundColor = Color(0xFF58D68D) // Verde
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contenido de opciones avanzadas (puedes agregar más opciones aquí)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Panel de Configuración",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aquí puedes agregar configuraciones avanzadas de la aplicación.",
                    fontSize = 18.sp,
                    color = Color(0xFF5D6D7E)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NoyaTheme {
        MainScreen()
    }
}