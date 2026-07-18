package com.toolbox.ddj.ui.screens.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.SystemMonitorRepository
import com.toolbox.ddj.data.model.MonitorSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SystemMonitorViewModel(
    private val repository: SystemMonitorRepository
) : ViewModel() {

    private val _sample = MutableStateFlow<MonitorSample?>(null)
    val sample: StateFlow<MonitorSample?> = _sample.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private var job: kotlinx.coroutines.Job? = null

    fun start() {
        if (_active.value) return
        _active.value = true
        job = viewModelScope.launch {
            repository.sample().collect { _sample.value = it }
        }
    }

    fun stop() {
        _active.value = false
        job?.cancel()
        job = null
    }

    override fun onCleared() {
        stop()
    }
}
