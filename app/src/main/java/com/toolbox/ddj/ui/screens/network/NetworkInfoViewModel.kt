package com.toolbox.ddj.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.NetworkInfo
import com.toolbox.ddj.data.NetworkInfoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 网络信息页 UI 状态：加载中标志 + 采集结果（未就绪时为 null）。
 */
data class NetworkInfoUiState(
    val loading: Boolean = true,
    val info: NetworkInfo? = null
)

class NetworkInfoViewModel(
    private val repository: NetworkInfoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkInfoUiState())
    val uiState: StateFlow<NetworkInfoUiState> = _uiState.asStateFlow()

    init {
        // 进入页面即采集一次
        refresh()
    }

    /** 重新采集当前网络信息 */
    fun refresh() {
        _uiState.value = _uiState.value.copy(loading = true)
        viewModelScope.launch {
            val info = repository.get()
            _uiState.value = NetworkInfoUiState(loading = false, info = info)
        }
    }
}
