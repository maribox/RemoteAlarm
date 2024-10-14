package it.bosler.remotealarm.ui.viewmodel

import android.bluetooth.BluetoothManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControlViewModel () : ViewModel() {
    private val _state = MutableStateFlow(ControlsScreenState());
    val state : StateFlow<ControlsScreenState> = _state.asStateFlow()
    // TODO: Load state from database/BLE connection

    // Events
    fun setIntensity(intensity: Float) {
        _state.value = _state.value.copy(intensity = intensity)
    }

    fun setCW_WW_Balance(cw_ww_balance: Float) {
        _state.value = _state.value.copy(cw_ww_balance = cw_ww_balance)
    }
}

data class ControlsScreenState (
    val connected: Boolean = false,
    val intensity: Float = .3f,
    val cw_ww_balance: Float = .5f,
)