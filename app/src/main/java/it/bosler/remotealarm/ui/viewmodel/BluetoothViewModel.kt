package it.bosler.remotealarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.juul.kable.Peripheral
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class BluetoothViewModel() : ViewModel() {
    private val _connectedPeripheral = MutableStateFlow<Peripheral?>(null)
    val test = "test${Instant.now().epochSecond}"
    init {
        println("BluetoothViewModel created")
    }
}