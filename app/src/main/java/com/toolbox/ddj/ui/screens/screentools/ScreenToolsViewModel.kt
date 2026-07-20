package com.toolbox.ddj.ui.screens.screentools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.StorageInfo
import com.toolbox.ddj.data.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 屏幕工具的子模式：主界面 / 坏点检测 / 触摸测试。
 */
enum class SubMode { NONE, DEAD_PIXEL, TOUCH }

/**
 * 屏幕 & 存储页 UI 状态。
 *
 * @param storage 存储信息，加载完成前为 null
 * @param subMode 当前处于哪个全屏工具（NONE 表示主界面）
 */
data class ScreenToolsUiState(
    val storage: StorageInfo? = null,
    val subMode: SubMode = SubMode.NONE
)

/**
 * 屏幕 & 存储页 ViewModel。
 *
 * 负责加载存储信息，并维护坏点检测 / 触摸测试的进入 / 退出状态。
 */
class ScreenToolsViewModel(
    private val repository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenToolsUiState())
    val uiState: StateFlow<ScreenToolsUiState> = _uiState.asStateFlow()

    init {
        // 进入页面即加载存储信息
        viewModelScope.launch {
            val info = runCatching { repository.get() }.getOrNull()
            _uiState.update { it.copy(storage = info) }
        }
    }

    /** 进入指定全屏工具。 */
    fun enter(mode: SubMode) {
        _uiState.update { it.copy(subMode = mode) }
    }

    /** 退出全屏工具，回到主界面。 */
    fun exit() {
        _uiState.update { it.copy(subMode = SubMode.NONE) }
    }
}
