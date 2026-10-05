package com.anils.sarjmetre

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest reading from the meter service, shared with the UI in the same process. */
object LiveState {
    private val _state = MutableStateFlow<MeterState?>(null)
    val state: StateFlow<MeterState?> = _state.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun publish(state: MeterState) {
        _state.value = state
    }

    fun setRunning(running: Boolean) {
        _running.value = running
        if (!running) _state.value = null
    }
}
