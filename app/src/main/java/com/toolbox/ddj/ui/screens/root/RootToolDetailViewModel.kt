package com.toolbox.ddj.ui.screens.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ddj.data.root.RootActionDef
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
import org.json.JSONArray
import org.json.JSONObject

class RootToolDetailViewModel(
    private val toolId: String,
    private val scriptManager: RootScriptManager
) : ViewModel() {

    data class UiState(
        val tool: RootToolDef? = null,
        val rootGranted: Boolean = false,
        val running: Boolean = false,
        val lastActionLabel: String? = null,
        val rawOutput: String = "",
        val prettyOutput: String = "",
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val tool = RootToolCatalog.byId(toolId)
        _uiState.value = UiState(
            tool = tool,
            rootGranted = RootUtils.isRootGranted()
        )
        // 进入详情时自动跑第一个“只读”动作（无需确认/输入）
        val auto = tool?.actions?.firstOrNull { !it.needsConfirm && !it.needsInput && !it.dangerous }
        if (auto != null && _uiState.value.rootGranted) {
            runAction(auto, null)
        }
    }

    fun runAction(action: RootActionDef, userInput: String?) {
        val tool = _uiState.value.tool ?: return
        if (action.needsInput && userInput.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入：${action.inputHint}")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                running = true,
                error = null,
                lastActionLabel = action.label
            )
            val result = withContext(Dispatchers.IO) {
                if (!RootUtils.isRootGranted()) {
                    return@withContext null to "未获得 Root 权限"
                }
                val args = action.buildArgs(userInput?.trim())
                val shell = scriptManager.runScript(
                    tool.script,
                    *args.toTypedArray(),
                    timeoutSec = action.timeoutSec
                )
                shell to null
            }
            val shell = result.first
            val err = result.second
            if (shell == null) {
                _uiState.value = _uiState.value.copy(
                    running = false,
                    error = err,
                    rootGranted = false
                )
                return@launch
            }
            val raw = shell.displayText()
            _uiState.value = _uiState.value.copy(
                running = false,
                rawOutput = raw,
                prettyOutput = tryPrettyJson(raw),
                error = if (shell.isSuccess) null else "退出码 ${shell.exitCode}" +
                    (if (shell.stderr.isNotBlank()) " · ${shell.stderr}" else ""),
                rootGranted = true
            )
        }
    }

    private fun tryPrettyJson(text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return text
        return try {
            when {
                t.startsWith("{") -> JSONObject(t).toString(2)
                t.startsWith("[") -> JSONArray(t).toString(2)
                else -> text
            }
        } catch (_: Exception) {
            text
        }
    }
}
