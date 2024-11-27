package it.bosler.remotealarm.bluetooth

import android.service.autofill.Validators.or
import android.util.Log
import com.benasher44.uuid.uuidFrom
import com.juul.kable.*
import com.juul.kable.logs.Logging
import com.juul.kable.logs.Logging.Level.Events
import com.juul.kable.logs.Logging.Level.Warnings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.max
import kotlin.math.round
import kotlin.system.exitProcess

private const val SCAN_DURATION_MILLIS = 10_000L // 10 seconds

private const val LIGHT_SERVICE_UUID = "b53e36d0-a21b-47b2-abac-343f523ff4d5"

private const val ALARM_ARRAY_CHARACTERISTIC_UUID = "a14af994-2a22-4762-b9e5-cb17a716645c"
private const val LIGHT_STATE_CHARACTERISTIC_UUID = "3c95cda9-7bde-471d-9c2b-ac0364befa78"
private const val TIMESTAMP_CHARACTERISTIC_UUID = "ab110e08-d3bb-4c8c-87a7-51d7076218cf"

class BluetoothManager {
    init {
        println("BluetoothManager created")
    }

    private val _connectedPeripheral = MutableStateFlow<Peripheral?>(null)
    val connectedPeripheralFlow: StateFlow<Peripheral?> = _connectedPeripheral.asStateFlow()
    private val connectedPeripheral: Peripheral?
        get() = _connectedPeripheral.value
    private val isPeripheralConnected: Boolean
        get() = _connectedPeripheral.value != null

    val connectionState : StateFlow<State>?
        get() = connectedPeripheral?.state

    private lateinit var peripheralScope: CoroutineScope

    private lateinit var alarmArrayChar: Characteristic
    private lateinit var lightStateChar: Characteristic
    private lateinit var timestampChar: Characteristic

    private val _lightState = MutableStateFlow(LightState())
    val lightState: StateFlow<LightState> = _lightState.asStateFlow()

    private val scanScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val foundCompatible = hashMapOf<String, PlatformAdvertisement>()
    private val foundIncompatible = hashMapOf<String, PlatformAdvertisement>()

    private val _status = MutableStateFlow<ScanStatus>(ScanStatus.Stopped)
    val status: StateFlow<ScanStatus> = _status.asStateFlow()

    private val _compatibleAdvertisements = MutableStateFlow<List<PlatformAdvertisement>>(emptyList())
    val compatibleAdvertisements: StateFlow<List<PlatformAdvertisement>> = _compatibleAdvertisements.asStateFlow()

    private val _incompatibleAdvertisements = MutableStateFlow<List<PlatformAdvertisement>>(emptyList())
    val incompatibleAdvertisements: StateFlow<List<PlatformAdvertisement>> = _incompatibleAdvertisements.asStateFlow()

    private val scanner = Scanner {
        logging {
            level = Events
        }
        filters {
            // Add any necessary filters here
        }
    }

    fun startScanning(autoConnect: Boolean = false) {
        if (_status.value == ScanStatus.Scanning) return // Scan already in progress.
        _status.value = ScanStatus.Scanning

        scanScope.launch {
            withTimeoutOrNull(SCAN_DURATION_MILLIS) {
                scanner
                    .advertisements
                    .catch { cause -> _status.value = ScanStatus.Failed(cause.message ?: "Unknown error") }
                    .onCompletion { cause -> if (cause == null || cause is CancellationException) _status.value = ScanStatus.Stopped }
                    .collect { advertisement ->
                        advertisement.uuids.forEach {
                            if (it == uuidFrom(LIGHT_SERVICE_UUID)) {
                                Log.v("BluetoothManager/Advertisements", "Found compatible device ${advertisement.name}")
                                foundCompatible[advertisement.address] = advertisement
                                _compatibleAdvertisements.value = foundCompatible.values.toList()
                                if (autoConnect) {
                                    if (connectionState == null || connectionState?.value is State.Disconnected) {
                                        connect(advertisement)
                                    }
                                }
                                return@collect
                            }
                        }

                        // Device does not have the required service UUID
                        if (advertisement.name != null) {
                            Log.d("BluetoothManager/Advertisements", "Found incompatible device ${advertisement.name}")
                            foundIncompatible[advertisement.address] = advertisement
                        }
                        _incompatibleAdvertisements.value = foundIncompatible.values.toList()
                    }
            }
        }
    }

    fun stopScanning() {
        scanScope.coroutineContext.cancelChildren()
    }

    fun clearScanResults() {
        stopScanning()
        foundCompatible.clear()
        foundIncompatible.clear()
        _compatibleAdvertisements.value = emptyList()
        _incompatibleAdvertisements.value = emptyList()
    }

    fun connect(advertisement: PlatformAdvertisement) {
        peripheralScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        _connectedPeripheral.value = peripheralScope.peripheral(advertisement) {
            logging {
                level = Warnings
            }
        }

        (connectedPeripheral ?: run {
            Log.e("BletoothManager/Peripheral", "Tried to connect to null peripheral")
            return
        }).state.onEach { state ->
            Log.i("BluetoothManager/State", "Received state: $state")
            if (state is State.Disconnected && state.status != null) {
                disconnectPeripheral()
            }
        }.launchIn(peripheralScope).apply {
            invokeOnCompletion { cause ->
                Log.w("BluetoothManager/State", "$cause - Auto connector complete")
            }
        }

        peripheralScope.launch {
            try {
                connectedPeripheral!!.connect()
                initializeCharacteristics()
                readInitialLightState()
                updateLightPeripheral()
            } catch (e: Exception) {
                Log.e("BluetoothManager/Peripheral", "Connecting to Peripheral failed. Reason: $e")
            }
        }
    }

    suspend fun disconnectPeripheral() {
        Log.d("BluetoothManager/State", "Disconnecting from Peripheral...")
        try {
            connectedPeripheral!!.disconnect()
        } catch (e: Exception) {
            Log.e("BluetoothManager/Peripheral", "Disconnecting from Peripheral failed. Reason: $e")
        }
        _connectedPeripheral.value = null
        peripheralScope.cancel()
    }

    private fun initializeCharacteristics() {
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

        Log.d("BluetoothManager/Char", "Characteristic: ${alarmArrayChar.characteristicUuid}")
        Log.d("BluetoothManager/Char", "Characteristic: ${lightStateChar.characteristicUuid}")
        Log.d("BluetoothManager/Char", "Characteristic: ${timestampChar.characteristicUuid}")
    }

    private suspend fun readInitialLightState() {
        var cw = 0
        var ww = 0
        try {
            val lightStateBytes = connectedPeripheral!!.read(lightStateChar)
            cw = lightStateBytes[0].toUByte().toInt()
            ww = lightStateBytes[1].toUByte().toInt()
        } catch (e: Exception) {
            Log.e("BluetoothManager/Peripheral", "Reading from Peripheral failed. Reason: $e")
        }

        Log.d("BluetoothManager/Char", "LightState on connect: $cw $ww")

        val intensity = (max(cw, ww).toDouble() / 255).coerceIn(0.0, 1.0)
        val balance = when {
            cw == 0 && ww == 0 -> 0.5
            cw == 0 -> 1.0
            ww == 0 -> 0.0
            else -> {
                if (cw >= ww)
                    ww.toDouble() / cw * 0.5
                else
                    (-cw).toDouble() / 255 / 2 + 1.0
            }
        }.coerceIn(0.0, 1.0)

        Log.d("BluetoothManager/Char", "Updated intensity/balance to: $intensity $balance")

        _lightState.value = LightState(
            intensity = intensity,
            cw_ww_balance = balance
        )
    }

    private suspend fun updateLightPeripheral() {
        Log.v("BluetoothManager/LightState", "Updated light peripheral")
        _lightState.value = _lightState.value.copy(
            cw_ww_balance = _lightState.value.cw_ww_balance.coerceIn(0.0, 1.0),
            intensity = _lightState.value.intensity.coerceIn(0.0, 1.0)
        )

        val (cwByte, wwByte) = calculateCW_WW_Bytes()

        if (!isPeripheralConnected || !peripheralScope.isActive) return
        try {
            connectedPeripheral!!.write(lightStateChar, byteArrayOf(cwByte, wwByte))
        } catch (e: Exception) {
            Log.e("BluetoothManager/Peripheral", "Writing to Peripheral failed. Disconnecting. Reason: $e")
            disconnectPeripheral()
        }
    }

    private fun calculateCW_WW_Bytes(): Pair<Byte, Byte> {
        val cw: Double
        val ww: Double
        if (_lightState.value.cw_ww_balance < 0.5) {
            cw = _lightState.value.intensity
            ww = _lightState.value.cw_ww_balance * 2 * _lightState.value.intensity
        } else {
            cw = (1 - _lightState.value.cw_ww_balance) * 2 * _lightState.value.intensity
            ww = _lightState.value.intensity
        }
        val cwByte = round(cw * 255).coerceIn(0.0, 255.0).toInt().toByte()
        val wwByte = round(ww * 255).coerceIn(0.0, 255.0).toInt().toByte()
        return cwByte to wwByte
    }

    suspend fun setIntensity(intensity: Double) {
        Log.v("BluetoothManager/LightState", "Setting intensity to $intensity")
        _lightState.value = _lightState.value.copy(intensity = intensity)
        if (isPeripheralConnected) {
            updateLightPeripheral()
        }
    }

    suspend fun setCW_WW_Balance(cw_ww_balance: Double) {
        Log.v("BluetoothManager/LightState", "Setting balance to $cw_ww_balance")
        _lightState.value = _lightState.value.copy(cw_ww_balance = cw_ww_balance)
        if (isPeripheralConnected) {
            updateLightPeripheral()
        }
    }
}

sealed class ScanStatus {
    object Stopped : ScanStatus()
    object Scanning : ScanStatus()
    data class Failed(val message: CharSequence) : ScanStatus()
}

data class LightState(
    val intensity: Double = .5,
    val cw_ww_balance: Double = .5, // cw = 0, ww = 1
)