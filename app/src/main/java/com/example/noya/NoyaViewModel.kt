package com.example.noya

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.noya.data.BatteryStatus
import com.example.noya.data.CallLog
import com.example.noya.data.Contact
import com.example.noya.data.ContactsCache
import com.example.noya.data.clearContactInfoCache
import com.example.noya.data.getContacts
import com.example.noya.data.getMissedCalls
import com.example.noya.device.getBatteryStatus
import com.example.noya.device.getCurrentDate
import com.example.noya.device.getCurrentTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel centralizado para manejo de estado
class NoyaViewModel(application: Application) : AndroidViewModel(application) {

    // Flag para evitar múltiples loops de actualización
    private var missedCallsUpdatesStarted = false

    // Observador de cambios en los contactos del teléfono (agregar, editar, sincronizar)
    private var contactsObserverRegistered = false
    private var contactsChangedJob: Job? = null
    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Esperar un momento: una sincronización genera muchos avisos seguidos
            contactsChangedJob?.cancel()
            contactsChangedJob = viewModelScope.launch {
                delay(1500)
                Log.d("NoyaViewModel", "Contactos cambiaron - actualizando nombres y fotos")
                clearContactInfoCache()
                ContactsCache.invalidate()
                refreshContacts()
                refreshMissedCalls()
            }
        }
    }

    // Estado de contactos
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(true)
    val isLoadingContacts = _isLoadingContacts.asStateFlow()

    // Estado de llamadas perdidas
    private val _missedCalls = MutableStateFlow<List<CallLog>>(emptyList())
    val missedCalls = _missedCalls.asStateFlow()

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
    private var hasCallLogPermission = false

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission = _hasContactsPermission.asStateFlow()

    // BroadcastReceiver para detectar cambios de carga instantáneamente
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED -> {
                    // Actualizar estado de batería inmediatamente
                    _batteryStatus.value = getBatteryStatus(getApplication())
                }
            }
        }
    }

    // BroadcastReceiver para detectar cambios de modo de sonido (incluye botones de volumen)
    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                syncSilentMode()
            }
        }
    }

    init {
        startTimeUpdates()
        startBatteryUpdates()
        registerBatteryReceiver()
        registerRingerModeReceiver()
        syncSilentMode()
    }

    // Registrar receiver para cambios de modo de sonido
    private fun registerRingerModeReceiver() {
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(ringerModeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            getApplication<Application>().registerReceiver(ringerModeReceiver, filter)
        }
    }

    // Registrar receiver para eventos de carga
    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            getApplication<Application>().registerReceiver(batteryReceiver, filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Desregistrar receiver para evitar memory leaks
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignorar si ya fue desregistrado
        }
        try {
            getApplication<Application>().unregisterReceiver(ringerModeReceiver)
        } catch (e: Exception) {
            // Ignorar si ya fue desregistrado
        }
        if (contactsObserverRegistered) {
            getApplication<Application>().contentResolver.unregisterContentObserver(contactsObserver)
        }
    }

    // Registrar el observador de contactos (requiere permiso de leer contactos)
    private fun registerContactsObserver() {
        if (contactsObserverRegistered) return
        val app = getApplication<Application>()
        val granted = ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        try {
            app.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver
            )
            contactsObserverRegistered = true
            // Recién hay permiso de contactos: volver a buscar nombres de llamadas perdidas
            clearContactInfoCache()
            refreshMissedCalls()
        } catch (e: Exception) {
            Log.e("NoyaViewModel", "Error registrando observador de contactos: ${e.message}", e)
        }
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

    // Actualizar nivel de batería cada 60 segundos (el estado de carga se detecta instantáneamente via BroadcastReceiver)
    private fun startBatteryUpdates() {
        viewModelScope.launch {
            while (true) {
                _batteryStatus.value = getBatteryStatus(getApplication())
                delay(60000)
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
        registerContactsObserver()

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
        if (!hasPermission) return
        hasCallLogPermission = true
        registerContactsObserver()

        // Si ya tenemos llamadas cargadas, no recargar
        if (_missedCalls.value.isNotEmpty()) return

        viewModelScope.launch {
            val calls = withContext(Dispatchers.IO) {
                getMissedCalls(getApplication())
            }
            _missedCalls.value = calls
        }
    }

    // Refrescar llamadas perdidas (llamar después de una llamada)
    fun refreshMissedCalls() {
        if (!hasCallLogPermission) return

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

    // Leer el modo de sonido real del teléfono (vibración cuenta como silencio)
    // y programar/cancelar la restauración automática del sonido
    fun syncSilentMode() {
        val app = getApplication<Application>()
        val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isSilent = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        _isSilentMode.value = isSilent

        if (isSilent) {
            SilentModeTimer.ensureScheduled(app)
        } else {
            SilentModeTimer.cancel(app)
        }
    }

    // Iniciar actualización periódica de llamadas perdidas - solo una vez
    fun startMissedCallsUpdates() {
        if (missedCallsUpdatesStarted) return
        missedCallsUpdatesStarted = true

        viewModelScope.launch {
            while (true) {
                delay(30000)
                if (hasCallLogPermission) {
                    val calls = withContext(Dispatchers.IO) {
                        getMissedCalls(getApplication())
                    }
                    _missedCalls.value = calls
                }
            }
        }
    }
}
