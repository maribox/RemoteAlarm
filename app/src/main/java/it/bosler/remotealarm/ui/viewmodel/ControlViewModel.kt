package it.bosler.remotealarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControlViewModel () : ViewModel() {
    private val _state = MutableStateFlow(ControlsScreenState());
    val state : StateFlow<ControlsScreenState> = _state.asStateFlow()

    // Events

}

data class ControlsScreenState (
    val connected: Boolean = false
)