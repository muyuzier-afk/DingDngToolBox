package com.toolbox.ddj.ui.screens.deviceinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.DeviceInfoRepository
import com.toolbox.ddj.data.model.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceInfoViewModel(
    private val repository: DeviceInfoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceInfoUiState>(DeviceInfoUiState.Loading)
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = DeviceInfoUiState.Loading
        viewModelScope.launch {
            _uiState.value = runCatching { repository.collect() }
                .fold(
                    onSuccess = { DeviceInfoUiState.Success(it) },
                    onFailure = { DeviceInfoUiState.Error(it.message ?: "未知错误") }
                )
        }
    }
}

sealed interface DeviceInfoUiState {
    data object Loading : DeviceInfoUiState
    data class Success(val data: DeviceInfo) : DeviceInfoUiState
    data class Error(val message: String) : DeviceInfoUiState
}
