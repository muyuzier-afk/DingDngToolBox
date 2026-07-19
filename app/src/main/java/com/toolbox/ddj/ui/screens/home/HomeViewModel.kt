package com.toolbox.ddj.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.BuildConfig
import com.toolbox.ddj.data.RemoteInfo
import com.toolbox.ddj.data.RemoteInfoRepository
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

    /** 当前应用版本号，用于与云端比较。 */
    val localVersionCode: Int = BuildConfig.VERSION_CODE

    private val remoteRepo = RemoteInfoRepository()
    private val _remote = MutableStateFlow<RemoteInfo?>(null)

    /** 云端版本 + 公告；拉取中或失败为 null（UI 回退本地文案）。 */
    val remote: StateFlow<RemoteInfo?> = _remote.asStateFlow()

    init {
        refreshPermissionState()
        viewModelScope.launch {
            _remote.value = remoteRepo.fetch()
        }
    }

    /** 云端版本号大于本地即有更新。 */
    fun hasUpdate(r: RemoteInfo?): Boolean = r != null && r.versionCode > localVersionCode

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
