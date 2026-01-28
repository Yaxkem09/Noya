package com.example.noya

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.size.Size
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.noya.ui.theme.NoyaTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

// Clase de datos para almacenar todas las dimensiones pre-calculadas
data class ResponsiveDimensValues(
    val scaleFactor: Float,
    val screenPadding: Dp,
    val smallPadding: Dp,
    val mediumPadding: Dp,
    val largePadding: Dp,
    val timeTextSize: Float,
    val dateTextSize: Float,
    val titleTextSize: Float,
    val largeTextSize: Float,
    val mediumTextSize: Float,
    val normalTextSize: Float,
    val smallTextSize: Float,
    val tinyTextSize: Float,
    val largeButtonHeight: Dp,
    val mediumButtonHeight: Dp,
    val smallButtonHeight: Dp,
    val extraSmallButtonHeight: Dp,
    val largeIconSize: Dp,
    val mediumIconSize: Dp,
    val smallIconSize: Dp,
    val contactPhotoSize: Dp,
    val callPhotoSize: Dp,
    val gridImageSize: Dp,
    val cornerRadius: Dp,
    val largeCornerRadius: Dp,
    val smallCornerRadius: Dp,
    val settingsButtonSize: Dp
)

// CompositionLocal para las dimensiones responsivas
private val LocalResponsiveDimens = staticCompositionLocalOf<ResponsiveDimensValues> {
    error("ResponsiveDimens not provided")
}

// Calcular dimensiones una sola vez
@Composable
private fun calculateResponsiveDimens(): ResponsiveDimensValues {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val scale = (screenWidth / 360f).coerceIn(0.8f, 1.5f)

    return ResponsiveDimensValues(
        scaleFactor = scale,
        screenPadding = (24 * scale).dp,
        smallPadding = (8 * scale).dp,
        mediumPadding = (16 * scale).dp,
        largePadding = (24 * scale).dp,
        timeTextSize = 56 * scale,
        dateTextSize = 24 * scale,
        titleTextSize = 32 * scale,
        largeTextSize = 28 * scale,
        mediumTextSize = 24 * scale,
        normalTextSize = 22 * scale,
        smallTextSize = 18 * scale,
        tinyTextSize = 16 * scale,
        largeButtonHeight = (100 * scale).dp,
        mediumButtonHeight = (80 * scale).dp,
        smallButtonHeight = (60 * scale).dp,
        extraSmallButtonHeight = (48 * scale).dp,
        largeIconSize = (48 * scale).dp,
        mediumIconSize = (32 * scale).dp,
        smallIconSize = (28 * scale).dp,
        contactPhotoSize = (150 * scale).dp,
        callPhotoSize = (150 * scale).dp,
        gridImageSize = (110 * scale).dp,
        cornerRadius = (16 * scale).dp,
        largeCornerRadius = (20 * scale).dp,
        smallCornerRadius = (12 * scale).dp,
        settingsButtonSize = (60 * scale).dp
    )
}

// Provider para las dimensiones
@Composable
fun ProvideResponsiveDimens(content: @Composable () -> Unit) {
    val dimens = calculateResponsiveDimens()
    CompositionLocalProvider(LocalResponsiveDimens provides dimens) {
        content()
    }
}

// Objeto para dimensiones responsivas - ahora lee del CompositionLocal (una sola lectura)
object ResponsiveDimens {
    val current: ResponsiveDimensValues
        @Composable get() = LocalResponsiveDimens.current

    @Composable
    fun getScaleFactor(): Float = current.scaleFactor

    @Composable
    fun screenPadding(): Dp = current.screenPadding

    @Composable
    fun smallPadding(): Dp = current.smallPadding

    @Composable
    fun mediumPadding(): Dp = current.mediumPadding

    @Composable
    fun largePadding(): Dp = current.largePadding

    @Composable
    fun timeTextSize(): Float = current.timeTextSize

    @Composable
    fun dateTextSize(): Float = current.dateTextSize

    @Composable
    fun titleTextSize(): Float = current.titleTextSize

    @Composable
    fun largeTextSize(): Float = current.largeTextSize

    @Composable
    fun mediumTextSize(): Float = current.mediumTextSize

    @Composable
    fun normalTextSize(): Float = current.normalTextSize

    @Composable
    fun smallTextSize(): Float = current.smallTextSize

    @Composable
    fun tinyTextSize(): Float = current.tinyTextSize

    @Composable
    fun largeButtonHeight(): Dp = current.largeButtonHeight

    @Composable
    fun mediumButtonHeight(): Dp = current.mediumButtonHeight

    @Composable
    fun smallButtonHeight(): Dp = current.smallButtonHeight

    @Composable
    fun extraSmallButtonHeight(): Dp = current.extraSmallButtonHeight

    @Composable
    fun largeIconSize(): Dp = current.largeIconSize

    @Composable
    fun mediumIconSize(): Dp = current.mediumIconSize

    @Composable
    fun smallIconSize(): Dp = current.smallIconSize

    @Composable
    fun contactPhotoSize(): Dp = current.contactPhotoSize

    @Composable
    fun callPhotoSize(): Dp = current.callPhotoSize

    @Composable
    fun gridImageSize(): Dp = current.gridImageSize

    @Composable
    fun cornerRadius(): Dp = current.cornerRadius

    @Composable
    fun largeCornerRadius(): Dp = current.largeCornerRadius

    @Composable
    fun smallCornerRadius(): Dp = current.smallCornerRadius

    @Composable
    fun settingsButtonSize(): Dp = current.settingsButtonSize
}

object IncomingCallState {
    var incomingCallNumber = mutableStateOf<String?>(null)
    var callEnded = mutableStateOf(false)
}

object ScreenNavigationState {
    var shouldNavigateToHome = mutableStateOf(false)
}

object AppSettings {
    var hideContactNames = mutableStateOf(false)
    var hideContactPhotos = mutableStateOf(false)
    var contactPhotoSize = mutableFloatStateOf(95f) // Tamaño por defecto en dp
}

// ViewModel centralizado para manejo de estado
class NoyaViewModel(application: Application) : AndroidViewModel(application) {

    // Flag para evitar múltiples loops de actualización
    private var missedCallsUpdatesStarted = false

    // Estado de contactos
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(true)
    val isLoadingContacts = _isLoadingContacts.asStateFlow()

    // Estado de llamadas perdidas
    private val _missedCalls = MutableStateFlow<List<CallLog>>(emptyList())
    val missedCalls = _missedCalls.asStateFlow()

    private val _isLoadingMissedCalls = MutableStateFlow(true)
    val isLoadingMissedCalls = _isLoadingMissedCalls.asStateFlow()

    // Estado de batería
    private val _batteryStatus = MutableStateFlow(BatteryStatus(0, false))
    val batteryStatus = _batteryStatus.asStateFlow()

    // Hora y fecha
    private val _currentTime = MutableStateFlow(getCurrentTime())
    val currentTime = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow(getCurrentDate())
    val currentDate = _currentDate.asStateFlow()

    // Modo silencio
    private val _isSilentMode = MutableStateFlow(false)
    val isSilentMode = _isSilentMode.asStateFlow()

    // Flag para indicar si tiene permisos
    private val _hasCallLogPermission = MutableStateFlow(false)
    val hasCallLogPermission = _hasCallLogPermission.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission = _hasContactsPermission.asStateFlow()

    init {
        startTimeUpdates()
        startBatteryUpdates()
    }

    // Actualizar hora cada segundo
    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = getCurrentTime()
                _currentDate.value = getCurrentDate()
                delay(1000)
            }
        }
    }

    // Actualizar batería cada 30 segundos
    private fun startBatteryUpdates() {
        viewModelScope.launch {
            while (true) {
                _batteryStatus.value = getBatteryStatus(getApplication())
                delay(30000)
            }
        }
    }

    // Cargar contactos - solo si no están ya cargados
    fun loadContacts(hasPermission: Boolean) {
        if (!hasPermission) {
            _isLoadingContacts.value = false
            return
        }
        _hasContactsPermission.value = true

        // Si ya tenemos contactos cargados, no recargar
        if (_contacts.value.isNotEmpty()) {
            _isLoadingContacts.value = false
            return
        }

        viewModelScope.launch {
            _isLoadingContacts.value = true
            val loadedContacts = withContext(Dispatchers.IO) {
                getContacts(getApplication())
            }
            _contacts.value = loadedContacts
            _isLoadingContacts.value = false
        }
    }

    // Cargar llamadas perdidas - solo si no están ya cargadas
    fun loadMissedCalls(hasPermission: Boolean) {
        if (!hasPermission) {
            _isLoadingMissedCalls.value = false
            return
        }
        _hasCallLogPermission.value = true

        // Si ya tenemos llamadas cargadas, no recargar
        if (_missedCalls.value.isNotEmpty()) {
            _isLoadingMissedCalls.value = false
            return
        }

        viewModelScope.launch {
            _isLoadingMissedCalls.value = true
            val calls = withContext(Dispatchers.IO) {
                getMissedCalls(getApplication())
            }
            _missedCalls.value = calls
            _isLoadingMissedCalls.value = false
        }
    }

    // Refrescar llamadas perdidas (llamar después de una llamada)
    fun refreshMissedCalls() {
        if (!_hasCallLogPermission.value) return

        viewModelScope.launch {
            val calls = withContext(Dispatchers.IO) {
                getMissedCalls(getApplication())
            }
            _missedCalls.value = calls
        }
    }

    // Refrescar contactos (llamar después de guardar un contacto)
    fun refreshContacts() {
        if (!_hasContactsPermission.value) return

        viewModelScope.launch {
            val loadedContacts = withContext(Dispatchers.IO) {
                getContacts(getApplication(), forceRefresh = true)
            }
            _contacts.value = loadedContacts
        }
    }

    // Actualizar modo silencio
    fun updateSilentMode(isSilent: Boolean) {
        _isSilentMode.value = isSilent
    }

    // Actualizar batería manualmente
    fun updateBattery() {
        viewModelScope.launch {
            _batteryStatus.value = getBatteryStatus(getApplication())
        }
    }

    // Iniciar actualización periódica de llamadas perdidas - solo una vez
    fun startMissedCallsUpdates() {
        if (missedCallsUpdatesStarted) return
        missedCallsUpdatesStarted = true

        viewModelScope.launch {
            while (true) {
                delay(30000)
                if (_hasCallLogPermission.value) {
                    val calls = withContext(Dispatchers.IO) {
                        getMissedCalls(getApplication())
                    }
                    _missedCalls.value = calls
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_SET_DEFAULT_DIALER = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ocultar la barra de estado del sistema (status bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

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

    // Solo navegar a home cuando la app VUELVE de estar en segundo plano
    // No usar onStop() porque se dispara durante gestos de navegación parciales
    override fun onRestart() {
        super.onRestart()
        Log.d("MainActivity", "onRestart llamado - navegando a home")
        ScreenNavigationState.shouldNavigateToHome.value = true
    }
}

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

@Composable
fun MainScreen() {
    // Proveer dimensiones responsivas una sola vez para toda la app
    ProvideResponsiveDimens {
        MainScreenContent()
    }
}

@Composable
private fun MainScreenContent() {
    val context = LocalContext.current
    // ViewModel compartido entre todas las pantallas
    val viewModel: NoyaViewModel = viewModel()

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
            // Refrescar llamadas perdidas al terminar una llamada
            viewModel.refreshMissedCalls()
        }
    }

    // Observar cuando debe navegar a home (pantalla suspendida)
    val shouldNavigateToHome by ScreenNavigationState.shouldNavigateToHome
    LaunchedEffect(shouldNavigateToHome) {
        if (shouldNavigateToHome) {
            Log.d("MainScreen", "Pantalla suspendida, regresando a home")
            currentScreen = "home"
            activeCallContact = null
            ScreenNavigationState.shouldNavigateToHome.value = false
        }
    }

    when (currentScreen) {
        "home" -> HomeScreen(
            onNavigateToContacts = { currentScreen = "contacts" },
            onNavigateToAdvancedOptions = { currentScreen = "advancedOptions" },
            onCallContact = { contact ->
                activeCallContact = contact
                currentScreen = "activeCall"
            },
            viewModel = viewModel
        )
        "contacts" -> ContactsScreen(
            onNavigateBack = { currentScreen = "home" },
            onCallStarted = { contact ->
                activeCallContact = contact
                currentScreen = "activeCall"
            },
            viewModel = viewModel
        )
        "newContact" -> NewContactScreen(
            onNavigateBack = { currentScreen = "advancedOptions" },
            viewModel = viewModel
        )
        "advancedOptions" -> AdvancedOptionsScreen(
            onNavigateBack = { currentScreen = "home" },
            onNavigateToNewContact = { currentScreen = "newContact" }
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
                // Refrescar llamadas perdidas al rechazar
                viewModel.refreshMissedCalls()
            }
        )
        "activeCall" -> activeCallContact?.let { contact ->
            ActiveCallScreen(
                contact = contact,
                onEndCall = {
                    currentScreen = "home"
                    // Refrescar llamadas perdidas al terminar
                    viewModel.refreshMissedCalls()
                }
            )
        }
    }
}

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
    val hasCallLogPermission by viewModel.hasCallLogPermission.collectAsStateWithLifecycle()

    // Estados locales de UI
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var isPressing by remember { mutableStateOf(false) }

    // Cargar llamadas perdidas al iniciar
    LaunchedEffect(Unit) {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.loadMissedCalls(readGranted)
        viewModel.startMissedCallsUpdates()
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
    val screenPadding = ResponsiveDimens.screenPadding()
    val smallPadding = ResponsiveDimens.smallPadding()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val largePadding = ResponsiveDimens.largePadding()
    val timeTextSize = ResponsiveDimens.timeTextSize()
    val dateTextSize = ResponsiveDimens.dateTextSize()
    val smallTextSize = ResponsiveDimens.smallTextSize()
    val mediumIconSize = ResponsiveDimens.mediumIconSize()
    val smallIconSize = ResponsiveDimens.smallIconSize()
    val settingsButtonSize = ResponsiveDimens.settingsButtonSize()

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
                    .padding(top = (60 * ResponsiveDimens.getScaleFactor()).dp, bottom = largePadding),
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
                text = "Llamar8",
                imageRes = R.drawable.ic_btn_llamar,
                onClick = { onNavigateToContacts() },
                modifier = Modifier.weight(1f)
            )

            // Botón Modo Silencio
            GridImageButton(
                text = if (isSilentMode) "Sonido" else "Silenciar",
                imageRes = if (isSilentMode) R.drawable.ic_btn_sonido else R.drawable.ic_btn_silenciar,
                onClick = {
                    toggleSilentMode(context) { newMode ->
                        viewModel.updateSilentMode(newMode)
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readContactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val callPhoneGranted = permissions[Manifest.permission.CALL_PHONE] ?: false

        if (readContactsGranted && callPhoneGranted) {
            viewModel.loadContacts(true)
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
    val screenPadding = ResponsiveDimens.screenPadding()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val titleTextSize = ResponsiveDimens.titleTextSize()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPadding)
    ) {
        // Título
        Text(
            text = "Contactos",
            fontSize = titleTextSize.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50), // Azul oscuro suave
            modifier = Modifier.padding(bottom = mediumPadding)
        )

        // Lista de contactos
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
                    Text(
                        text = "No hay contactos",
                        fontSize = mediumTextSize.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(mediumPadding)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
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
}

@Composable
fun ContactItem(contact: Contact, onCallClick: () -> Unit) {
    val context = LocalContext.current
    val hideNames by AppSettings.hideContactNames
    val hidePhotos by AppSettings.hideContactPhotos
    val photoSize by AppSettings.contactPhotoSize

    // Dimensiones responsivas
    val scaleFactor = ResponsiveDimens.getScaleFactor()
    val smallPadding = ResponsiveDimens.smallPadding()
    val smallCornerRadius = ResponsiveDimens.smallCornerRadius()
    val cornerRadius = ResponsiveDimens.cornerRadius()
    val normalTextSize = ResponsiveDimens.normalTextSize()
    val tinyTextSize = ResponsiveDimens.tinyTextSize()

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

// Caché de contactos con timestamp para invalidación
private object ContactsCache {
    var contacts = emptyList<Contact>()
    var lastUpdate: Long = 0
    const val CACHE_DURATION_MS = 60_000L // 60 segundos

    fun isValid(): Boolean {
        return contacts.isNotEmpty() &&
            (System.currentTimeMillis() - lastUpdate) < CACHE_DURATION_MS
    }

    fun update(newContacts: List<Contact>) {
        contacts = newContacts
        lastUpdate = System.currentTimeMillis()
    }

    fun invalidate() {
        contacts = emptyList()
        lastUpdate = 0
    }
}

fun getContacts(context: Context, forceRefresh: Boolean = false): List<Contact> {
    // Retornar caché si es válido y no se fuerza actualización
    if (!forceRefresh && ContactsCache.isValid()) {
        return ContactsCache.contacts
    }

    val contacts = mutableListOf<Contact>()
    val seenNumbers = mutableSetOf<String>() // Para evitar duplicados

    val cursor: Cursor? = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
        val photoUriIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

        while (it.moveToNext()) {
            val id = it.getString(idIndex)
            val name = it.getString(nameIndex) ?: "Sin nombre"
            val number = it.getString(numberIndex) ?: ""
            val photoUri = if (photoUriIndex >= 0) it.getString(photoUriIndex) else null

            // Normalizar el número (quitar espacios, guiones, paréntesis)
            val normalizedNumber = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")

            if (number.isNotEmpty() && !seenNumbers.contains(normalizedNumber)) {
                seenNumbers.add(normalizedNumber)
                contacts.add(Contact(id, name, number, photoUri))
            }
        }
    }

    // Actualizar caché
    ContactsCache.update(contacts)

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
    var contactPhoto by remember { mutableStateOf<String?>(contact.photoUri) }

    // Obtener la foto del contacto si no la tenemos (en IO thread)
    LaunchedEffect(contact.phoneNumber) {
        if (contactPhoto == null) {
            val (_, photoUri) = withContext(Dispatchers.IO) {
                getContactInfoFromNumber(context, contact.phoneNumber)
            }
            contactPhoto = photoUri
        }
    }

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
            audioManager.isSpeakerphoneOn = false
            onEndCall()
        }
    }

    // Iniciar la llamada cuando se muestra la pantalla (solo para llamadas salientes)
    LaunchedEffect(contact.id) {
        // Configurar el modo de audio al iniciar
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            Log.d("ActiveCallScreen", "Modo de audio configurado a IN_COMMUNICATION")
        } catch (e: Exception) {
            Log.e("ActiveCallScreen", "Error al configurar modo de audio: ${e.message}", e)
        }

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

            // Limpiar configuración de audio
            try {
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
                Log.d("ActiveCallScreen", "Audio restaurado a modo normal")
            } catch (e: Exception) {
                Log.e("ActiveCallScreen", "Error al restaurar audio: ${e.message}", e)
            }

            // Limpiar el callback para evitar memory leaks
            CallManager.setCallStateCallback { }
        }
    }

    // Dimensiones responsivas
    val screenPadding = ResponsiveDimens.screenPadding()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val largePadding = ResponsiveDimens.largePadding()
    val callPhotoSize = ResponsiveDimens.callPhotoSize()
    val largeCornerRadius = ResponsiveDimens.largeCornerRadius()
    val largeTextSize = ResponsiveDimens.largeTextSize()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()
    val largeIconSize = ResponsiveDimens.largeIconSize()
    val largeButtonHeight = ResponsiveDimens.largeButtonHeight()
    val scaleFactor = ResponsiveDimens.getScaleFactor()

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
                    contentDescription = "Foto de ${contact.name}",
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
            text = contact.name,
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
                    // Usar MODE_IN_COMMUNICATION para mejor compatibilidad con altavoz
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

                    if (isSpeakerOn) {
                        // Activar altavoz
                        audioManager.isSpeakerphoneOn = true
                        // Subir volumen de comunicación al máximo
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
                        Log.d("ActiveCallScreen", "Altavoz activado - Modo: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}, Vol: $maxVolume")
                    } else {
                        // Desactivar altavoz
                        audioManager.isSpeakerphoneOn = false
                        Log.d("ActiveCallScreen", "Altavoz desactivado - Modo: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}")
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
                    imageVector = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
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
                audioManager.isSpeakerphoneOn = false
                CallManager.endCall()
                onEndCall()
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
                    imageVector = Icons.Filled.Call,
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

@Composable
fun IncomingCallScreen(
    callerNumber: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current
    var callerName by remember { mutableStateOf<String?>(null) }
    var callerPhoto by remember { mutableStateOf<String?>(null) }
    var hasEnded by remember { mutableStateOf(false) }

    // Buscar el nombre y foto del contacto (en IO thread)
    LaunchedEffect(callerNumber) {
        val (name, photoUri) = withContext(Dispatchers.IO) {
            getContactInfoFromNumber(context, callerNumber)
        }
        callerName = name
        callerPhoto = photoUri
    }

    // Monitorear si la llamada fue cancelada via callback
    LaunchedEffect(Unit) {
        CallManager.setCallStateCallback { state ->
            Log.d("IncomingCallScreen", "Estado de llamada actualizado: $state")

            // Si la llamada se desconectó o cambió de estado antes de contestar
            if (!hasEnded && (state == Call.STATE_DISCONNECTED ||
                (state != Call.STATE_RINGING && state != Call.STATE_ACTIVE))) {
                Log.d("IncomingCallScreen", "Llamada cancelada/terminada (estado: $state) - cerrando pantalla")
                hasEnded = true
                IncomingCallState.incomingCallNumber.value = null
                IncomingCallState.callEnded.value = true
            }
        }
    }

    // Verificación inicial: si no hay llamada entrante al mostrar la pantalla
    LaunchedEffect(Unit) {
        delay(100) // Pequeño margen para que se establezca el estado

        val currentCall = CallManager.ongoingCall
        if (currentCall == null && !hasEnded) {
            Log.d("IncomingCallScreen", "No hay llamada activa al entrar - cerrando pantalla")
            hasEnded = true
            IncomingCallState.incomingCallNumber.value = null
            IncomingCallState.callEnded.value = true
        }
    }

    // Mantener la pantalla encendida
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        onDispose {
            Log.d("IncomingCallScreen", "Pantalla de llamada entrante destruida - limpiando recursos")
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            // Limpiar el callback
            CallManager.setCallStateCallback { }
        }
    }

    // Dimensiones responsivas
    val screenPadding = ResponsiveDimens.screenPadding()
    val smallPadding = ResponsiveDimens.smallPadding()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val largePadding = ResponsiveDimens.largePadding()
    val callPhotoSize = ResponsiveDimens.callPhotoSize()
    val largeCornerRadius = ResponsiveDimens.largeCornerRadius()
    val largeTextSize = ResponsiveDimens.largeTextSize()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()
    val titleTextSize = ResponsiveDimens.titleTextSize()
    val largeIconSize = ResponsiveDimens.largeIconSize()
    val largeButtonHeight = ResponsiveDimens.largeButtonHeight()
    val scaleFactor = ResponsiveDimens.getScaleFactor()

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
                    imageVector = Icons.Filled.Call,
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

    // Dimensiones responsivas
    val smallPadding = ResponsiveDimens.smallPadding()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val smallButtonHeight = ResponsiveDimens.smallButtonHeight()
    val extraSmallButtonHeight = ResponsiveDimens.extraSmallButtonHeight()
    val mediumButtonHeight = ResponsiveDimens.mediumButtonHeight()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()
    val smallTextSize = ResponsiveDimens.smallTextSize()
    val largeTextSize = ResponsiveDimens.largeTextSize()
    val normalTextSize = ResponsiveDimens.normalTextSize()
    val mediumIconSize = ResponsiveDimens.mediumIconSize()
    val scaleFactor = ResponsiveDimens.getScaleFactor()

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
                                saveContactWithPhoto(context, contactName, phoneNumber, selectedPhotoUri)
                                viewModel.refreshContacts() // Actualizar lista de contactos
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
                            text = "Guardar Contacto",
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

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    customButtonHeight: Dp? = null,
    customTextSize: Float? = null
) {
    // Dimensiones responsivas
    val smallPadding = ResponsiveDimens.smallPadding()
    val smallButtonHeight = ResponsiveDimens.smallButtonHeight()
    val largeTextSize = ResponsiveDimens.largeTextSize()
    val smallIconSize = ResponsiveDimens.smallIconSize()

    val buttonHeight = customButtonHeight ?: smallButtonHeight
    val textSize = customTextSize ?: largeTextSize

    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(smallPadding)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                row.forEach { digit ->
                    Button(
                        onClick = { onNumberClick(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34495E), // Azul grisáceo oscuro pero suave
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = digit,
                            fontSize = textSize.sp,
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
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE67E22), // Naranja más suave
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Backspace,
                    contentDescription = "Borrar",
                    modifier = Modifier.size(smallIconSize)
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
        // Invalidar caché de contactos para que se recarguen
        ContactsCache.invalidate()
    } catch (e: Exception) {
        Toast.makeText(context, "Error guardando contacto: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e("SaveContact", "Error: ${e.message}", e)
    }
}

fun saveContactWithPhoto(context: Context, name: String, phoneNumber: String, photoUri: Uri?) {
    try {
        // Crear contacto en el teléfono (no SIM para poder guardar foto)
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

        // Insertar foto si existe
        if (photoUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(photoUri)
                val photoBytes = inputStream?.readBytes()
                inputStream?.close()

                if (photoBytes != null) {
                    val photoValues = android.content.ContentValues().apply {
                        put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                    }
                    context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, photoValues)
                }
            } catch (e: Exception) {
                Log.e("SaveContact", "Error guardando foto: ${e.message}", e)
            }
        }

        Toast.makeText(context, "Contacto guardado", Toast.LENGTH_SHORT).show()
        // Invalidar caché de contactos para que se recarguen
        ContactsCache.invalidate()

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

fun getContactInfoFromNumber(context: Context, phoneNumber: String): Pair<String?, String?> {
    try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI
            ),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIndex = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                val name = if (nameIndex >= 0) it.getString(nameIndex) else null
                val photoUri = if (photoIndex >= 0) it.getString(photoIndex) else null
                return Pair(name, photoUri)
            }
        }
    } catch (e: Exception) {
        Log.e("GetContactInfo", "Error: ${e.message}", e)
    }
    return Pair(null, null)
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
    return sdf.format(Date())
}

data class BatteryStatus(val level: Int, val isCharging: Boolean)

fun getBatteryStatus(context: Context): BatteryStatus {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                     status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryStatus(level, isCharging)
}

fun getBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
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
    backgroundColor: Color = Color(0xFF5DADE2), // Azul claro y suave por defecto
    modifier: Modifier = Modifier,
    customHeight: Dp? = null,
    customIconSize: Dp? = null,
    customTextSize: Float? = null
) {
    // Dimensiones responsivas
    val largeButtonHeight = ResponsiveDimens.largeButtonHeight()
    val largeIconSize = ResponsiveDimens.largeIconSize()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()
    val buttonHeight = customHeight ?: largeButtonHeight
    val iconSize = customIconSize ?: largeIconSize
    val textSize = customTextSize ?: mediumTextSize

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
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
                modifier = Modifier.size(iconSize),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(mediumPadding))
            Text(
                text = text,
                fontSize = textSize.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun LargeImageButton(
    text: String,
    @DrawableRes imageRes: Int,
    onClick: () -> Unit,
    backgroundColor: Color = Color(0xFF5DADE2)
) {
    // Dimensiones responsivas
    val scaleFactor = ResponsiveDimens.getScaleFactor()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height((120 * scaleFactor).dp),
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
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = text,
                modifier = Modifier.size((64 * scaleFactor).dp)
            )
            Spacer(modifier = Modifier.width(mediumPadding))
            Text(
                text = text,
                fontSize = mediumTextSize.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun GridImageButton(
    text: String,
    @DrawableRes imageRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF5DADE2)
) {
    // Dimensiones responsivas
    val cornerRadius = ResponsiveDimens.cornerRadius()
    val smallPadding = ResponsiveDimens.smallPadding()
    val gridImageSize = ResponsiveDimens.gridImageSize()
    val smallTextSize = ResponsiveDimens.smallTextSize()

    Button(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f), // Hace el botón cuadrado
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius), // Esquinas redondeadas, no circular
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        contentPadding = PaddingValues(smallPadding)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = text,
                modifier = Modifier.size(gridImageSize)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = smallTextSize.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
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
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val smallPadding = ResponsiveDimens.smallPadding()
    val cornerRadius = ResponsiveDimens.cornerRadius()
    val smallIconSize = ResponsiveDimens.smallIconSize()
    val normalTextSize = ResponsiveDimens.normalTextSize()
    val smallTextSize = ResponsiveDimens.smallTextSize()

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
                    imageVector = Icons.Filled.CallMissed,
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
    val scaleFactor = ResponsiveDimens.getScaleFactor()
    val smallPadding = ResponsiveDimens.smallPadding()
    val smallCornerRadius = ResponsiveDimens.smallCornerRadius()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()
    val tinyTextSize = ResponsiveDimens.tinyTextSize()
    val mediumIconSize = ResponsiveDimens.mediumIconSize()

    // Tamaño de foto para el panel (escalado)
    val panelPhotoSize = (photoSize * 0.8f * scaleFactor).coerceIn(70f * scaleFactor, 120f * scaleFactor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = smallPadding),
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
                containerColor = Color.White,
                contentColor = Color(0xFF58D68D)
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

// Cache para información de contactos - evita consultas repetidas
private val contactInfoCache = mutableMapOf<String, Pair<String?, String?>>()

fun getContactInfoBatch(context: Context, phoneNumbers: Set<String>): Map<String, Pair<String?, String?>> {
    val result = mutableMapOf<String, Pair<String?, String?>>()
    val numbersToQuery = mutableSetOf<String>()

    // Primero revisar caché
    for (number in phoneNumbers) {
        val cached = contactInfoCache[number]
        if (cached != null) {
            result[number] = cached
        } else {
            numbersToQuery.add(number)
        }
    }

    // Solo consultar los que no están en caché
    if (numbersToQuery.isEmpty()) return result

    try {
        // Consulta batch: obtener todos los contactos del dispositivo una sola vez
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            null
        )

        // Crear mapa de números normalizados a info de contacto
        val contactsMap = mutableMapOf<String, Pair<String?, String?>>()
        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex) ?: continue
                val normalizedNumber = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
                val name = if (nameIndex >= 0) it.getString(nameIndex) else null
                val photoUri = if (photoIndex >= 0) it.getString(photoIndex) else null
                contactsMap[normalizedNumber] = Pair(name, photoUri)
            }
        }

        // Mapear los números solicitados a sus contactos
        for (number in numbersToQuery) {
            val normalizedNumber = number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
            val contactInfo = contactsMap[normalizedNumber] ?: Pair(null, null)
            result[number] = contactInfo
            contactInfoCache[number] = contactInfo // Guardar en caché
        }

    } catch (e: Exception) {
        Log.e("GetContactInfoBatch", "Error: ${e.message}", e)
        // Si falla, devolver null para todos
        for (number in numbersToQuery) {
            result[number] = Pair(null, null)
        }
    }

    return result
}

fun getMissedCalls(context: Context): List<CallLog> {
    val missedCalls = mutableListOf<CallLog>()

    try {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        // Datos temporales de llamadas perdidas (sin info de contacto aún)
        data class TempCallData(val id: String, val number: String, val dateMillis: Long)
        val tempMissedCalls = mutableListOf<TempCallData>()

        // 1. Obtener llamadas perdidas (solo datos básicos)
        context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            arrayOf(
                android.provider.CallLog.Calls._ID,
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.DATE
            ),
            "${android.provider.CallLog.Calls.TYPE} = ? AND ${android.provider.CallLog.Calls.DATE} >= ?",
            arrayOf(android.provider.CallLog.Calls.MISSED_TYPE.toString(), twentyFourHoursAgo.toString()),
            "${android.provider.CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(android.provider.CallLog.Calls._ID)
            val numberIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val dateIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE)

            while (cursor.moveToNext()) {
                tempMissedCalls.add(TempCallData(
                    id = cursor.getString(idIndex),
                    number = cursor.getString(numberIndex) ?: "Desconocido",
                    dateMillis = cursor.getLong(dateIndex)
                ))
            }
        }

        // 2. Obtener llamadas salientes para filtrar las devueltas
        val outgoingCalls = mutableListOf<Pair<String, Long>>()
        context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            arrayOf(
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.DATE
            ),
            "${android.provider.CallLog.Calls.TYPE} = ? AND ${android.provider.CallLog.Calls.DATE} >= ?",
            arrayOf(android.provider.CallLog.Calls.OUTGOING_TYPE.toString(), twentyFourHoursAgo.toString()),
            null
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val dateIndex = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex) ?: ""
                if (number.isNotEmpty()) {
                    outgoingCalls.add(Pair(
                        number.replace(Regex("[\\s\\-\\(\\)\\.]"), ""),
                        cursor.getLong(dateIndex)
                    ))
                }
            }
        }

        // 3. Filtrar llamadas no devueltas
        val unreturned = tempMissedCalls.filter { missed ->
            val normalizedMissed = missed.number.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
            !outgoingCalls.any { (outNum, outDate) ->
                outNum == normalizedMissed && outDate > missed.dateMillis
            }
        }

        // 4. Obtener info de contactos en BATCH (una sola consulta)
        val uniqueNumbers = unreturned.map { it.number }.toSet()
        val contactsInfo = getContactInfoBatch(context, uniqueNumbers)

        // 5. Construir la lista final
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (call in unreturned) {
            val (name, photoUri) = contactsInfo[call.number] ?: Pair(null, null)
            missedCalls.add(CallLog(
                id = call.id,
                phoneNumber = call.number,
                name = name,
                date = dateFormat.format(Date(call.dateMillis)),
                time = timeFormat.format(Date(call.dateMillis)),
                photoUri = photoUri
            ))
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
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readCallLogGranted = permissions[Manifest.permission.READ_CALL_LOG] ?: false
        val callPhoneGranted = permissions[Manifest.permission.CALL_PHONE] ?: false

        if (readCallLogGranted && callPhoneGranted) {
            hasPermission = true
            coroutineScope.launch {
                val calls = withContext(Dispatchers.IO) {
                    getMissedCalls(context)
                }
                missedCalls = calls
                isLoading = false
            }
        } else {
            isLoading = false
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
            val calls = withContext(Dispatchers.IO) {
                getMissedCalls(context)
            }
            missedCalls = calls
            isLoading = false
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
                    items(missedCalls, key = { it.id }) { callLog ->
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

    // Dimensiones responsivas
    val screenPadding = ResponsiveDimens.screenPadding()
    val smallPadding = ResponsiveDimens.smallPadding()
    val mediumPadding = ResponsiveDimens.mediumPadding()
    val largePadding = ResponsiveDimens.largePadding()
    val mediumButtonHeight = ResponsiveDimens.mediumButtonHeight()
    val mediumIconSize = ResponsiveDimens.mediumIconSize()
    val cornerRadius = ResponsiveDimens.cornerRadius()
    val titleTextSize = ResponsiveDimens.titleTextSize()
    val mediumTextSize = ResponsiveDimens.mediumTextSize()
    val normalTextSize = ResponsiveDimens.normalTextSize()
    val smallTextSize = ResponsiveDimens.smallTextSize()
    val tinyTextSize = ResponsiveDimens.tinyTextSize()
    val scaleFactor = ResponsiveDimens.getScaleFactor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPadding)
    ) {
        // Botón de regresar
        val extraSmallButtonHeight = ResponsiveDimens.extraSmallButtonHeight()
        val smallIconSize = ResponsiveDimens.smallIconSize()
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

        // Botón Configuraciones del Teléfono
        val smallButtonHeight = ResponsiveDimens.smallButtonHeight()
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
                        onCheckedChange = { hideNames = it },
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
                        onCheckedChange = { hidePhotos = it },
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
                    onValueChange = { photoSize = it },
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
                            if (Settings.System.canWrite(context)) {
                                Settings.System.putInt(
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                                )
                                Settings.System.putInt(
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    newBrightness.toInt()
                                )
                            } else {
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            }
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NoyaTheme {
        MainScreen()
    }
}