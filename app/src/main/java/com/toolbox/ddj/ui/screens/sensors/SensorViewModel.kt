package com.toolbox.ddj.ui.screens.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.SensorItem
import com.toolbox.ddj.data.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 传感器页 ViewModel。
 *
 * - [sensors] 全部传感器静态列表，构造时读取一次即可。
 * - [readings] 主要传感器的实时读数快照；在 viewModelScope 内 collect，
 *   scope 取消（页面销毁）时协程随之取消，底层 callbackFlow 自动注销监听。
 */
class SensorViewModel(
    private val repository: SensorRepository
) : ViewModel() {

    /** 全部传感器静态信息，一次性读取。 */
    val sensors: List<SensorItem> = repository.list()

    private val _readings = MutableStateFlow<Map<String, String>>(emptyMap())
    val readings: StateFlow<Map<String, String>> = _readings.asStateFlow()

    init {
        // 订阅实时读数：每份快照直接覆盖到 StateFlow
        viewModelScope.launch {
            repository.liveReadings().collect { _readings.value = it }
        }
    }
}
