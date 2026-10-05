package com.anils.sarjmetre

import com.anils.sarjmetre.net.NetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest reading from the meter service, shared with the UI in the same process. */
object LiveState {
    /** Samples kept for the live chart: two minutes at the default one-second interval. */
    const val HISTORY_SIZE = 120

    private val _state = MutableStateFlow<MeterState?>(null)
    val state: StateFlow<MeterState?> = _state.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _history = MutableStateFlow<List<Int>>(emptyList())
    val history: StateFlow<List<Int>> = _history.asStateFlow()

    /** Null while the network meter is off. */
    private val _net = MutableStateFlow<NetState?>(null)
    val net: StateFlow<NetState?> = _net.asStateFlow()

    fun publishNet(state: NetState?) {
        _net.value = state
    }

    fun publish(state: MeterState) {
        _state.value = state
    }

    /** One point per timer tick, so the chart's time axis stays even. */
    fun appendSample(currentMa: Int) {
        _history.value = (_history.value + currentMa).takeLast(HISTORY_SIZE)
    }

    fun setRunning(running: Boolean) {
        _running.value = running
        if (!running) {
            _state.value = null
            _history.value = emptyList()
            _net.value = null
        }
    }
}
