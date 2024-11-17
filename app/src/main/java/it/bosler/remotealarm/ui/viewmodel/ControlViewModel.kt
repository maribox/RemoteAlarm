package it.bosler.remotealarm.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuidFrom
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.Scanner
import com.juul.kable.State as PeripheralConnectionState
import com.juul.kable.State.Disconnected
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging.Level.Events
import com.juul.kable.logs.Logging.Level.Warnings
import com.juul.kable.peripheral
import it.bosler.remotealarm.ui.viewmodel.ScanStatus.Scanning
import it.bosler.remotealarm.ui.viewmodel.ScanStatus.Stopped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.math.round

private val SCAN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(10)

private const val LIGHT_SERVICE_UUID = "b53e36d0-a21b-47b2-abac-343f523ff4d5"

private const val ALARM_ARRAY_CHARACTERISTIC_UUID = "a14af994-2a22-4762-b9e5-cb17a716645c"
private const val LIGHT_STATE_CHARACTERISTIC_UUID = "3c95cda9-7bde-471d-9c2b-ac0364befa78"
private const val TIMESTAMP_CHARACTERISTIC_UUID = "ab110e08-d3bb-4c8c-87a7-51d7076218cf"

class ControlViewModel () : ViewModel() {

    private val _uiState = MutableStateFlow(ControlsScreenState())
    val uiState : StateFlow<ControlsScreenState> = _uiState.asStateFlow()

    fun setScanPane(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(scanPaneExpanded = expanded)
        foundCompatible.clear()
        foundIncompatible.clear()
        _compatibleAdvertisements.value = emptyList()
        _incompatibleAdvertisements.value = emptyList()
    }

    private val _connectedPeripheral = MutableStateFlow<Peripheral?>(null)
    val connectedPeripheralFlow = _connectedPeripheral.asStateFlow()
    private val connectedPeripheral : Peripheral
        get() = _connectedPeripheral.value?: throw UninitializedPropertyAccessException("Peripheral has not been initialized yet.")
    private val isPeripheralConnected : Boolean
        get() = _connectedPeripheral.value != null

    val connectionState : StateFlow<PeripheralConnectionState>
        get() {
            return if (isPeripheralConnected) {
                connectedPeripheral.state
            } else {
                MutableStateFlow(Disconnected(null))
            }
        }

    private lateinit var peripheralScope : CoroutineScope

    private lateinit var alarmArrayChar : Characteristic
    private lateinit var lightStateChar : Characteristic
    private lateinit var timestampChar : Characteristic

    // TODO: Load state from database/BLE connection
    private val _lightState = MutableStateFlow(LightState());
    val lightState : StateFlow<LightState> = _lightState.asStateFlow()

    private val scanScope = viewModelScope.childScope()
    private val foundCompatible = hashMapOf<String, PlatformAdvertisement>()
    private val foundIncompatible = hashMapOf<String, PlatformAdvertisement>()

    private val _status = MutableStateFlow<ScanStatus>(Stopped)
    val status = _status.asStateFlow()

    private val _compatibleAdvertisements = MutableStateFlow<List<PlatformAdvertisement>>(emptyList())
    val compatibleAdvertisements = _compatibleAdvertisements.asStateFlow()

    private val _incompatibleAdvertisements = MutableStateFlow<List<PlatformAdvertisement>>(emptyList())
    val incompatibleAdvertisements = _incompatibleAdvertisements.asStateFlow()

    // Events
    fun start() {
        Log.v("Control/Scan", "Start")
        if (status.value == Scanning) return // Scan already in progress.
        _status.value = Scanning

        scanScope.launch {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                scanner
                    .advertisements
                    .catch { cause -> _status.value = ScanStatus.Failed(cause.message ?: "Unknown error") }
                    .onCompletion { cause -> if (cause == null || cause is java.util.concurrent.CancellationException) _status.value = Stopped }
                    .collect { advertisement ->
                        advertisement.uuids.forEach {
                            if (it == uuidFrom(LIGHT_SERVICE_UUID)) {
                                Log.v(
                                    "Control/Advertisements",
                                    "found correct device ${advertisement.name}"
                                )
                                foundCompatible[advertisement.address] = advertisement
                                _compatibleAdvertisements.value = foundCompatible.values.toList()
                                return@collect
                            }
                        }

                        // valid Service-UUID has not been found
                        if (advertisement.name != null) {
                            Log.d("Control/Advertisements", "found incompatible device ${advertisement.name}")
                            foundIncompatible[advertisement.address] = advertisement
                        }
                        _incompatibleAdvertisements.value = foundIncompatible.values.toList()
                    }
            }
        }
    }

    fun stop() {
        scanScope.cancelChildren()
    }

    fun clear() {
        stop()
        _compatibleAdvertisements.value = emptyList()
        _incompatibleAdvertisements.value = emptyList()
    }


    fun connect(advertisement: PlatformAdvertisement) {
        peripheralScope = CoroutineScope(Job())
        // TODO: If input is now made, app will crash because peripheral can't be written to
        _connectedPeripheral.value = peripheralScope.peripheral(advertisement) {
            logging {
                level = Warnings
            }
        }

        connectedPeripheral.state.onEach { state ->
            Log.i("Control/State", "Received state: $state")
            if (state is Disconnected && state.status != null) {
                disconnectPeripheral()
            }
        }.launchIn(viewModelScope).apply {
            invokeOnCompletion { cause ->
                Log.w("Control/State",  "$cause - Auto connector complete")
            }
        }

        peripheralScope.launch {
            try {
                connectedPeripheral.connect()
            } catch (e: Exception) {
                Log.e("Control/Peripheral", "Connectiong to Peripheral failed. Reason: $e")
            }
            setScanPane(false)

            alarmArrayChar = characteristicOf(
                service = LIGHT_SERVICE_UUID,
                characteristic = ALARM_ARRAY_CHARACTERISTIC_UUID,
            )
            lightStateChar = characteristicOf(
                service = LIGHT_SERVICE_UUID,
                characteristic = LIGHT_STATE_CHARACTERISTIC_UUID,
            )
            timestampChar = characteristicOf(
                service = LIGHT_SERVICE_UUID,
                characteristic = TIMESTAMP_CHARACTERISTIC_UUID,
            )

            Log.d("Connect/Char", "Characteristic: ${alarmArrayChar.characteristicUuid}")
            Log.d("Connect/Char", "Characteristic: ${lightStateChar.characteristicUuid}")
            Log.d("Connect/Char", "Characteristic: ${timestampChar.characteristicUuid}")
            var cw = 0
            var ww = 0
            try {
                var lightStateBytes = connectedPeripheral.read(lightStateChar)
                cw = lightStateBytes.get(0).toUByte().toInt()
                ww = lightStateBytes.get(1).toUByte().toInt()
            } catch (e: Exception) {
                Log.e("Control/Peripheral", "Reading from Peripheral failed. Reason: $e")
            }

            Log.d("Connect/Char", "LightState on connect: $cw $ww")

            _lightState.value = LightState(
                intensity = (cw + ww) / 255.0 / 2,
                cw_ww_balance = if (cw + ww != 0) cw / (cw + ww).toDouble() else 0.5
            )

            updateLightPeripheral()
        }
    }

    private suspend fun updateLightPeripheral() {
        _lightState.value.copy(cw_ww_balance = _lightState.value.cw_ww_balance.coerceIn(0.0, 1.0))
        _lightState.value.copy(intensity = _lightState.value.intensity.coerceIn(0.0, 1.0))

        var cw: Double
        var ww: Double
        if (lightState.value.cw_ww_balance >= 0.5) {
            cw = _lightState.value.intensity
            ww = (1 - lightState.value.cw_ww_balance) * _lightState.value.intensity * 2
        } else {
            cw = lightState.value.cw_ww_balance * _lightState.value.intensity * 2
            ww = _lightState.value.intensity
        }
        var cwByte = round(cw * 255).coerceIn(0.0, 255.0).toInt().toByte()
        var wwByte = round(ww * 255).coerceIn(0.0, 255.0).toInt().toByte()
        if (!isPeripheralConnected || !peripheralScope.isActive) return
        try {
            connectedPeripheral.write(lightStateChar, byteArrayOf(cwByte, wwByte))
        } catch (e: Exception) {
            Log.e("Control/Peripheral", "Writing to Peripheral failed. Disconnecting. Reason: $e")
            disconnectPeripheral()
        }
    }

    private suspend fun disconnectPeripheral() {
        Log.d("Control/State", "Disconnecting from Peripheral...")
        try {
            connectedPeripheral.disconnect()
        } catch (e: Exception) {
            Log.e("Control/Peripheral", "Disconnecting from Peripheral failed. Reason: $e")
        }
        _connectedPeripheral.value = null
        peripheralScope.cancel()
    }

    fun setIntensity(intensity: Double) {
        _lightState.value = _lightState.value.copy(intensity = intensity)
        if (isPeripheralConnected) {
            peripheralScope.launch {
                updateLightPeripheral()
            }
        }
    }

    fun setCW_WW_Balance(cw_ww_balance: Double) {
        _lightState.value = _lightState.value.copy(cw_ww_balance = cw_ww_balance)
        if (isPeripheralConnected) {
            peripheralScope.launch {
                updateLightPeripheral()
            }
        }
    }


    fun onScanPaneClicked() {
        if (isPeripheralConnected && !uiState.value.scanPaneExpanded) {
            peripheralScope.launch {
                disconnectPeripheral()
            }
        }
        setScanPane(!uiState.value.scanPaneExpanded)
    }
}

val scanner = Scanner {
    logging {
        level = Events
    }
    filters {
    }
}


sealed class ScanStatus {
    object Stopped : ScanStatus()
    object Scanning : ScanStatus()
    data class Failed(val message: CharSequence) : ScanStatus()
}

data class LightState (
    val intensity: Double = .3,
    val cw_ww_balance: Double = .5,
)

data class ControlsScreenState (
    val scanPaneExpanded: Boolean = false,
)

fun CoroutineScope.childScope() =
    CoroutineScope(coroutineContext + Job(coroutineContext[Job]))

fun CoroutineScope.cancelChildren(
    cause: CancellationException? = null
) = coroutineContext[Job]?.cancelChildren(cause)
