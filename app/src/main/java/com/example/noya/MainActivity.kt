package com.example.noya

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.noya.data.Contact
import com.example.noya.ui.ProvideResponsiveDimens
import com.example.noya.ui.screens.ActiveCallScreen
import com.example.noya.ui.screens.AdvancedOptionsScreen
import com.example.noya.ui.screens.ContactsScreen
import com.example.noya.ui.screens.HomeScreen
import com.example.noya.ui.screens.IncomingCallScreen
import com.example.noya.ui.screens.NewContactScreen
import com.example.noya.ui.theme.NoyaTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_SET_DEFAULT_DIALER = 1001
    }

    private var forceCallUiVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Siempre habilitar mostrar sobre lock screen ANTES de super.onCreate
        // para que la ventana se cree con los flags correctos
        enableShowOverLockScreenEarly()

        super.onCreate(savedInstanceState)

        // Cargar configuraciones guardadas
        AppSettings.init(this)

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CallManager.callState.collectLatest { state ->
                    val shouldForceShow = state == Call.STATE_RINGING || state == Call.STATE_ACTIVE
                    if (shouldForceShow && !forceCallUiVisible) {
                        forceCallUiVisible = true
                        enableShowOverLockScreen()
                    } else if (!shouldForceShow && forceCallUiVisible) {
                        forceCallUiVisible = false
                        disableShowOverLockScreen()
                    }
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
            if (it.getBooleanExtra("INCOMING_CALL", false)) {
                enableShowOverLockScreen()
            }
            handleIncomingCallIntent(it)
        }
    }

    override fun onResume() {
        super.onResume()
        // Si hay una llamada activa timbrando, asegurar que se muestra sobre lock screen
        if (CallManager.ringingCall() != null) {
            enableShowOverLockScreen()
        }
    }

    private fun handleIncomingCallIntent(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra("INCOMING_CALL", false)) {
                val callerNumber = it.getStringExtra("CALLER_NUMBER") ?: "Desconocido"
                Log.d("MainActivity", "Llamada entrante detectada: $callerNumber")
                // Limpiar callEnded de llamadas anteriores ANTES de notificar la nueva
                IncomingCallState.callEnded.value = false
                IncomingCallState.incomingCallNumber.value = callerNumber
                // Se tocó "Contestar" en la notificación
                if (it.getBooleanExtra(IncomingCallNotification.EXTRA_ANSWER_CALL, false)) {
                    Log.d("MainActivity", "Contestar desde la notificación")
                    IncomingCallState.answerRequested.value = true
                }
                // Limpiar el intent
                it.removeExtra("INCOMING_CALL")
                it.removeExtra("CALLER_NUMBER")
                it.removeExtra(IncomingCallNotification.EXTRA_ANSWER_CALL)
            }
        }
    }

    // Se llama ANTES de super.onCreate() - solo usa APIs que no requieren window
    private fun enableShowOverLockScreenEarly() {
        Log.d("MainActivity", "Habilitando mostrar sobre lock screen (early)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    fun enableShowOverLockScreen() {
        Log.d("MainActivity", "Habilitando mostrar sobre pantalla de bloqueo")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Pedir desbloqueo del keyguard
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    fun disableShowOverLockScreen() {
        Log.d("MainActivity", "Deshabilitando mostrar sobre pantalla de bloqueo")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        }
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
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
        // No salir de la pantalla de llamada si hay una llamada en curso
        if (CallManager.ongoingCall != null) {
            Log.d("MainActivity", "onRestart llamado - hay llamada en curso, no navegar a home")
            return
        }
        Log.d("MainActivity", "onRestart llamado - navegando a home")
        ScreenNavigationState.shouldNavigateToHome.value = true
    }
}

@Composable
fun MainScreen() {
    // Proveer dimensiones responsivas una sola vez para toda la app
    ProvideResponsiveDimens {
        MainScreenContent()
    }
}

@Composable
private fun MainScreenContent() {
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
            // No navegar a home si hay una nueva llamada entrante
            if (incomingCallerNumber != null) {
                Log.d("MainScreen", "Llamada terminada, pero hay nueva llamada entrante - no navegar a home")
                IncomingCallState.callEnded.value = false
            } else {
                Log.d("MainScreen", "Llamada terminada, regresando a home")
                currentScreen = "home"
                activeCallContact = null
                IncomingCallState.callEnded.value = false
                // Refrescar llamadas perdidas al terminar una llamada
                viewModel.refreshMissedCalls()
            }
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

    // Si la llamada con la que se habla cambia (por ejemplo, al colgar la segunda
    // llamada se reanuda la que estaba en espera), mostrar los datos de esa llamada
    val activeCallNumber by CallManager.activeCallNumber.collectAsStateWithLifecycle()
    LaunchedEffect(activeCallNumber) {
        val number = activeCallNumber ?: return@LaunchedEffect
        val contact = activeCallContact ?: return@LaunchedEffect
        if (currentScreen == "activeCall" && !PhoneNumberUtils.compare(contact.phoneNumber, number)) {
            Log.d("MainScreen", "La llamada activa cambió a $number")
            activeCallContact = Contact("incoming", number, number)
        }
    }

    // Al terminar o rechazar una llamada que sonaba: si sigue otra llamada
    // en curso, volver a su pantalla; si no, regresar a home
    val returnAfterIncomingCall = {
        IncomingCallState.incomingCallNumber.value = null
        if (CallManager.activeCall() != null) {
            // Sigue otra llamada, o se contestó por otro lado (audífonos, notificación)
            Log.d("MainScreen", "Hay una llamada en curso - mostrando la pantalla de llamada")
            if (activeCallContact == null) {
                val number = CallManager.activeCallNumber.value ?: "Desconocido"
                activeCallContact = Contact("incoming", number, number)
            }
            currentScreen = "activeCall"
        } else {
            currentScreen = "home"
            activeCallContact = null
            viewModel.refreshMissedCalls()
        }
    }

    val acceptIncomingCall = {
        CallManager.answerCall()
        val contact = Contact("incoming", incomingCallerNumber ?: "Desconocido", incomingCallerNumber ?: "")
        activeCallContact = contact
        IncomingCallState.incomingCallNumber.value = null
        currentScreen = "activeCall"
    }

    // Contestar desde la notificación de llamada entrante
    val answerRequested by IncomingCallState.answerRequested
    LaunchedEffect(answerRequested) {
        if (answerRequested) {
            IncomingCallState.answerRequested.value = false
            if (CallManager.ringingCall() != null) {
                Log.d("MainScreen", "Contestando llamada desde la notificación")
                acceptIncomingCall()
            }
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
            onNavigateToNewContact = { currentScreen = "newContact" },
            viewModel = viewModel
        )
        "incomingCall" -> IncomingCallScreen(
            callerNumber = incomingCallerNumber ?: "Desconocido",
            onAccept = acceptIncomingCall,
            onReject = {
                CallManager.rejectCall()
                returnAfterIncomingCall()
            },
            onCallGone = { returnAfterIncomingCall() }
        )
        "activeCall" -> activeCallContact?.let { contact ->
            // key: si cambia la llamada activa, la pantalla se crea de nuevo con sus datos
            key(contact.phoneNumber) {
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
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NoyaTheme {
        MainScreen()
    }
}
