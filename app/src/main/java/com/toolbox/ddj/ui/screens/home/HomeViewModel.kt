package com.toolbox.ddj.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.BuildConfig
import com.toolbox.ddj.util.RootUtils
import com.toolbox.ddj.util.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    data class PermissionState(
        val rootGranted: Boolean = false,
        val shizukuInstalled: Boolean = false,
        val shizukuRunning: Boolean = false,
        val shizukuGranted: Boolean = false
    )

    private val _permState = MutableStateFlow(PermissionState())
    val permState: StateFlow<PermissionState> = _permState.asStateFlow()

    /** 当前应用版本名。 */
    val versionName: String = BuildConfig.VERSION_NAME

    init {
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                PermissionState(
                    rootGranted = RootUtils.isRootGranted(),
                    shizukuInstalled = ShizukuUtils.isShizukuInstalled(com.toolbox.ddj.DingDongJiApp.instance),
                    shizukuRunning = ShizukuUtils.isShizukuRunning(),
                    shizukuGranted = ShizukuUtils.isShizukuGranted()
                )
            }
            _permState.value = state
        }
    }
}
