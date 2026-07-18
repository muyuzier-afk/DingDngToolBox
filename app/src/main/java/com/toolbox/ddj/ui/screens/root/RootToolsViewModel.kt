package com.toolbox.ddj.ui.screens.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.root.RootCategory
import com.toolbox.ddj.data.root.RootScriptManager
import com.toolbox.ddj.data.root.RootToolCatalog
import com.toolbox.ddj.data.root.RootToolDef
import com.toolbox.ddj.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RootToolsViewModel(
    private val scriptManager: RootScriptManager
) : ViewModel() {

    data class UiState(
        val rootGranted: Boolean = false,
        val deploying: Boolean = false,
        val deployMessage: String? = null,
        val deployOk: Boolean? = null,
        val toolsByCategory: Map<RootCategory, List<RootToolDef>> = emptyMap()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        _uiState.value = UiState(toolsByCategory = RootToolCatalog.byCategory())
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val granted = withContext(Dispatchers.IO) { RootUtils.isRootGranted() }
            _uiState.value = _uiState.value.copy(rootGranted = granted)
            if (granted) {
                deploy()
            } else {
                _uiState.value = _uiState.value.copy(
                    deployOk = false,
                    deployMessage = "未获得 Root，ROOT 功能不可用"
                )
            }
        }
    }

    fun deploy() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deploying = true, deployMessage = "正在部署脚本…")
            val result = withContext(Dispatchers.IO) { scriptManager.ensureDeployed() }
            _uiState.value = _uiState.value.copy(
                deploying = false,
                deployOk = result.ok,
                deployMessage = result.message,
                rootGranted = RootUtils.isRootGranted()
            )
        }
    }
}
