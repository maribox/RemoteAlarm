package it.bosler.remotealarm.domain

import android.R.attr.action
import android.util.Log
import com.benasher44.uuid.uuidFrom
import com.juul.kable.*
import com.juul.kable.logs.Logging.Level.Events
import com.juul.kable.logs.Logging.Level.Warnings
import it.bosler.remotealarm.data.Alarms.Alarm
import it.bosler.remotealarm.data.Alarms.Schedule.SpecificMoment
import it.bosler.remotealarm.data.Alarms.Schedule.WeekdaysWithLocalTime
import it.bosler.remotealarm.data.Alarms.ScheduleType
import it.bosler.remotealarm.shared.toBytes
import it.bosler.remotealarm.shared.toFormattedHex
import it.bosler.remotealarm.ui.viewmodel.toLightProgramBytes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.round

private const val SCAN_DURATION_MILLIS = 10_000L // 10 seconds

private const val LIGHT_SERVICE_UUID = "b53e36d0-a21b-47b2-abac-343f523ff4d5"

private const val ALARM_ARRAY_CHARACTERISTIC_UUID = "a14af994-2a22-4762-b9e5-cb17a716645c"
private const val LIGHT_STATE_CHARACTERISTIC_UUID = "3c95cda9-7bde-471d-9c2b-ac0364befa78"
private const val TIMESTAMP_CHARACTERISTIC_UUID = "ab110e08-d3bb-4c8c-87a7-51d7076218cf"

class BluetoothManager {
    init {
        println("BluetoothManager created")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectedPeripheral = MutableStateFlow<Peripheral?>(null)
    val connectedPeripheralFlow: StateFlow<Peripheral?> = _connectedPeripheral.asStateFlow()
    private val connectedPeripheral: Peripheral?
        get() = _connectedPeripheral.value
    private val isPeripheralConnected: Boolean
        get() = _connectedPeripheral.value != null

    val connectionState : StateFlow<State>?
        get() = connectedPeripheral?.state

    private var alarmArrayChar: Characteristic? = null
    private var lightStateChar: Characteristic? = null
    private var timestampChar: Characteristic? = null

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
                                // TODO why is this printed so often?
                                Log.v("BluetoothManager/Advertisements", "Found compatible device ${advertisement.name}")
                                foundCompatible[advertisement.address] = advertisement
                                _compatibleAdvertisements.value = foundCompatible.values.toList()
                                if (autoConnect) {
                                    if (connectionState == null || connectionState?.value is State.Disconnected) {
                                        connect(advertisement)
                                        cancel()
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
        _connectedPeripheral.value = scope.peripheral(advertisement) {
            logging {
                level = Warnings
            }
            onServicesDiscovered {
                requestMtu(512)
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
        }.launchIn(scope).apply {
            invokeOnCompletion { cause ->
                Log.w("BluetoothManager/State", "$cause - Auto connector complete")
            }
        }

        scope.launch {
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

        Log.d("BluetoothManager/Char", "Characteristic: ${alarmArrayChar?.characteristicUuid}")
        Log.d("BluetoothManager/Char", "Characteristic: ${lightStateChar?.characteristicUuid}")
        Log.d("BluetoothManager/Char", "Characteristic: ${timestampChar?.characteristicUuid}")
    }

    private suspend fun readInitialLightState() {
        var cw = 0
        var ww = 0
        try {
            val lightStateBytes = connectedPeripheral!!.read(lightStateChar!!)
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
            colorTemperatureBalance = balance
        )
    }

    private fun updateLightPeripheral() {
        _lightState.value = _lightState.value.copy(
            colorTemperatureBalance = _lightState.value.colorTemperatureBalance.coerceIn(0.0, 1.0),
            intensity = _lightState.value.intensity.coerceIn(0.0, 1.0)
        )

        val (cwByte, wwByte) = calculateLightStateBytes()
        if (lightStateChar == null || !isPeripheralConnected || !scope.isActive) return
        writeToPeripheral(lightStateChar!!, byteArrayOf(cwByte, wwByte))
    }

    private fun calculateLightStateBytes(): Pair<Byte, Byte> {
        return calculateLightStateBytes(_lightState.value.intensity, _lightState.value.colorTemperatureBalance)
    }

    companion object {
        fun calculateLightStateBytes(intensity: Double, colorTemperatureBalance: Double): Pair<Byte, Byte> {
            val cw: Double
            val ww: Double
            if (colorTemperatureBalance < 0.5) {
                cw = intensity
                ww = colorTemperatureBalance * 2 * intensity
            } else {
                cw = (1 - colorTemperatureBalance) * 2 * intensity
                ww = intensity
            }
            val cwByte = round(cw * 255).coerceIn(0.0, 255.0).toInt().toByte()
            val wwByte = round(ww * 255).coerceIn(0.0, 255.0).toInt().toByte()
            return cwByte to wwByte
        }
    }

    suspend fun setIntensity(intensity: Double) {
        _lightState.value = _lightState.value.copy(intensity = intensity)
        if (isPeripheralConnected) {
            updateLightPeripheral()
        }
    }

    suspend fun setColorTemperatureBalance(colorTemperatureBalance: Double) {
        _lightState.value = _lightState.value.copy(colorTemperatureBalance = colorTemperatureBalance)
        if (isPeripheralConnected) {
            updateLightPeripheral()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun addAlarm(alarm: Alarm) {
        if (alarmArrayChar == null) {
            Log.e("BluetoothManager/Char", "AlarmArrayChar not initialized. Cannot write.")
            return
        }
        syncTime()
        var alarmBytes : ByteArray = byteArrayOf()
        if (alarm.schedule is SpecificMoment) {
            alarmBytes += alarm.schedule.time.toEpochSecond().toBytes()
            alarmBytes += alarm.action.toLightProgramBytes()
            writeToPeripheral(alarmArrayChar!!, alarmBytes)
        } else if (alarm.schedule is WeekdaysWithLocalTime) {
            throw NotImplementedError("WeekdaysWithLocalTime not implemented yet")
        }
        println("Sent ${alarmBytes.size} bytes")
        println("Sent bytes: ${alarmBytes.toFormattedHex()}")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun syncTime() {
        if (timestampChar == null) {
            Log.e("BluetoothManager/Char", "TimestampChar not initialized. Cannot write.")
            return
        }
        val timestamp = ZonedDateTime.now().toEpochSecond()
        val timestampBytes = timestamp.toBytes()
        Log.d("BluetoothManager/Char", "Syncing time: $timestamp with bytes: ${timestampBytes.toFormattedHex()}")
        writeToPeripheral(timestampChar!!, timestampBytes)
    }

    private fun writeToPeripheral(characteristic: Characteristic, bytes: ByteArray) {
        if (connectedPeripheral == null || !isPeripheralConnected) {
            Log.e("BluetoothManager/Peripheral", "Peripheral not connected. Cannot write.")
            return
        }
        connectedPeripheral?.let {
            scope.launch {
                try {
                    it.write(characteristic, bytes)
                } catch (e: Exception) {
                    Log.e("BluetoothManager/Peripheral", "Writing to Peripheral failed. Disconnecting. Reason: $e")
                    disconnectPeripheral()
                }
            }
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
    val colorTemperatureBalance: Double = .5, // cw = 0, ww = 1
)