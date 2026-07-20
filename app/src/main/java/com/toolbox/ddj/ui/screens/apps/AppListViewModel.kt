package com.toolbox.ddj.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.AppInfo
import com.toolbox.ddj.data.AppListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 应用列表过滤维度。
 */
enum class AppFilter { ALL, USER, SYSTEM }

/**
 * 应用管理页 UI 状态。
 *
 * 采用「一次性全量加载 + 内存过滤」策略：[all] 只加载一次，
 * 切换 [filter] 无需重新读取系统，[visible] 即为当前应展示的列表。
 *
 * @param loading 是否正在加载
 * @param all 全量应用
 * @param filter 当前过滤维度
 */
data class AppListUiState(
    val loading: Boolean = true,
    val all: List<AppInfo> = emptyList(),
    val filter: AppFilter = AppFilter.ALL
) {
    /** 依据 [filter] 派生的可见列表。 */
    fun visible(): List<AppInfo> = when (filter) {
        AppFilter.ALL -> all
        AppFilter.USER -> all.filter { !it.isSystem }
        AppFilter.SYSTEM -> all.filter { it.isSystem }
    }
}

class AppListViewModel(
    private val repository: AppListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        // 进入即加载全部应用，失败兜底为空列表，避免页面卡在 loading
        viewModelScope.launch {
            val apps = runCatching { repository.list() }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(loading = false, all = apps)
        }
    }

    /** 切换过滤维度。 */
    fun setFilter(f: AppFilter) {
        _uiState.value = _uiState.value.copy(filter = f)
    }
}
