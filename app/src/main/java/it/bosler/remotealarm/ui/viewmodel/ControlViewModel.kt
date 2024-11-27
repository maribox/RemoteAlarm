package it.bosler.remotealarm.ui.viewmodel

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.juul.kable.PlatformAdvertisement
import it.bosler.remotealarm.bluetooth.BluetoothManager
import it.bosler.remotealarm.bluetooth.LightState
import it.bosler.remotealarm.bluetooth.ScanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.juul.kable.State

class ControlViewModel(
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    init {
        println("ControlViewModel created")
    }

    companion object {
        fun get_factory(bluetoothManager: BluetoothManager) = object : ViewModelProvider.Factory
        {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                if (modelClass.isAssignableFrom(ControlViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ControlViewModel(bluetoothManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    private val _uiState = MutableStateFlow(ControlsScreenState())
    val uiState: StateFlow<ControlsScreenState> = _uiState

    val lightState = bluetoothManager.lightState
    val connectionState : StateFlow<State>?
        get() = bluetoothManager.connectionState
    val connectedPeripheralFlow = bluetoothManager.connectedPeripheralFlow
    val status = bluetoothManager.status
    val compatibleAdvertisements = bluetoothManager.compatibleAdvertisements
    val incompatibleAdvertisements = bluetoothManager.incompatibleAdvertisements


    // Events
    fun startScan() {
        bluetoothManager.startScanning()
    }

    fun stopScan() {
        bluetoothManager.stopScanning()
    }

    fun clearScanResults() {
        bluetoothManager.clearScanResults()
    }

    fun tryConnect() {
        bluetoothManager.startScanning(true)
    }

    fun connect(advertisement: PlatformAdvertisement) {
        bluetoothManager.connect(advertisement)
        setScanPane(false)
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothManager.disconnectPeripheral()
        }
    }

    fun setIntensity(intensity: Double) {
        viewModelScope.launch {
            bluetoothManager.setIntensity(intensity)
        }
    }

    fun setCW_WW_Balance(cw_ww_balance: Double) {
        viewModelScope.launch {
            bluetoothManager.setCW_WW_Balance(cw_ww_balance)
        }
    }

    fun setScanPane(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(scanPaneExpanded = expanded)
        bluetoothManager.clearScanResults()
    }

    fun onScanPaneClicked() {
        if (connectedPeripheralFlow.value != null && !uiState.value.scanPaneExpanded) {
            disconnect()
        }
        setScanPane(!uiState.value.scanPaneExpanded)
    }
}

data class ControlsScreenState(
    val scanPaneExpanded: Boolean = false,
)