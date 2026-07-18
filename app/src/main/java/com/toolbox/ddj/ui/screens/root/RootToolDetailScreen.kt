package com.toolbox.ddj.ui.screens.root

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toolbox.ddj.DingDongJiApp
import com.toolbox.ddj.R
import com.toolbox.ddj.data.root.RootActionDef
import com.toolbox.ddj.data.root.RootScriptManager
import com.toolbox.ddj.ui.components.StatusBanner
import com.toolbox.ddj.ui.components.TonalCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootToolDetailScreen(
    toolId: String,
    onBack: () -> Unit,
    viewModel: RootToolDetailViewModel = viewModel(
        key = toolId,
        factory = viewModelFactory {
            initializer {
                RootToolDetailViewModel(
                    toolId = toolId,
                    scriptManager = RootScriptManager(DingDongJiApp.instance)
                )
            }
        }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tool = state.tool
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var pendingAction by remember { mutableStateOf<RootActionDef?>(null) }
    var inputAction by remember { mutableStateOf<RootActionDef?>(null) }
    var inputValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(tool?.title ?: stringResource(R.string.tool_root_tools)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { padding ->
        if (tool == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.root_tool_not_found), color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!state.rootGranted) {
                StatusBanner(message = stringResource(R.string.root_deploy_need_root))
            }

            Text(
                text = stringResource(R.string.root_actions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            // 操作按钮区：横向可滚 + 自动换行风格的流式 Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tool.actions.chunked(2).forEach { rowActions ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowActions.forEach { action ->
                            val btnModifier = Modifier.weight(1f)
                            if (action.dangerous) {
                                Button(
                                    onClick = {
                                        when {
                                            action.needsInput -> {
                                                inputAction = action
                                                inputValue = ""
                                            }
                                            action.needsConfirm -> pendingAction = action
                                            else -> viewModel.runAction(action, null)
                                        }
                                    },
                                    enabled = state.rootGranted && !state.running,
                                    modifier = btnModifier
                                ) { Text(action.label) }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        when {
                                            action.needsInput -> {
                                                inputAction = action
                                                inputValue = ""
                                            }
                                            action.needsConfirm -> pendingAction = action
                                            else -> viewModel.runAction(action, null)
                                        }
                                    },
                                    enabled = state.rootGranted && !state.running,
                                    modifier = btnModifier
                                ) { Text(action.label) }
                            }
                        }
                        if (rowActions.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (state.running) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.padding(8.dp))
                    Text(
                        text = stringResource(
                            R.string.root_running,
                            state.lastActionLabel ?: ""
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state.error?.let {
                StatusBanner(
                    message = it,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            TonalCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.root_output),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.lastActionLabel != null) {
                        Text(
                            text = state.lastActionLabel!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.prettyOutput.ifBlank {
                            stringResource(R.string.root_output_empty)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // 确认对话框
    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = {
                Text(
                    if (action.dangerous) stringResource(R.string.root_confirm_danger_title)
                    else stringResource(R.string.root_confirm_title)
                )
            },
            text = {
                Text(
                    stringResource(
                        if (action.dangerous) R.string.root_confirm_danger_msg
                        else R.string.root_confirm_msg,
                        action.label
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction = null
                    viewModel.runAction(action, null)
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 输入对话框
    inputAction?.let { action ->
        AlertDialog(
            onDismissRequest = { inputAction = null },
            title = { Text(action.label) },
            text = {
                Column {
                    if (action.needsConfirm || action.dangerous) {
                        Text(
                            text = stringResource(
                                if (action.dangerous) R.string.root_confirm_danger_msg
                                else R.string.root_confirm_msg,
                                action.label
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text(action.inputHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val a = action
                        inputAction = null
                        viewModel.runAction(a, inputValue)
                    },
                    enabled = inputValue.isNotBlank()
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { inputAction = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
