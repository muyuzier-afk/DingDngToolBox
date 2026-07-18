package com.toolbox.ddj.ui.screens.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toolbox.ddj.DingDongJiApp
import com.toolbox.ddj.R
import com.toolbox.ddj.data.SystemMonitorRepository
import com.toolbox.ddj.ui.components.InfoRow
import com.toolbox.ddj.ui.components.InfoSectionCard
import com.toolbox.ddj.ui.components.UsageBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SystemMonitorScreen(
    onBack: () -> Unit,
    viewModel: SystemMonitorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SystemMonitorViewModel(SystemMonitorRepository(DingDongJiApp.instance)) }
        }
    )
) {
    // 进入页面即开始采样，离开时 ViewModel.onCleared 自动停止
    LaunchedEffect(Unit) { viewModel.start() }

    val sample by viewModel.sample.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tool_system_monitor)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (active) viewModel.stop() else viewModel.start() },
                icon = {
                    Icon(
                        if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                },
                text = { Text(if (active) "暂停" else "开始") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val s = sample
            // CPU
            InfoSectionCard(title = stringResource(R.string.monitor_cpu), icon = Icons.Filled.Speed) {
                if (s == null) {
                    Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    UsageBar(
                        label = stringResource(R.string.monitor_usage),
                        percent = s.cpuUsagePercent
                    )
                    InfoRow("逻辑核心数", s.coreFreqMHz.size.toString())
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        s.coreFreqMHz.forEachIndexed { index, mhz ->
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(if (mhz > 0) "C$index $mhz MHz" else "C$index -")
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
            // 内存
            InfoSectionCard(title = stringResource(R.string.monitor_memory), icon = Icons.Filled.Memory) {
                if (s == null) {
                    Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    UsageBar(label = stringResource(R.string.monitor_usage), percent = s.memUsagePercent)
                    InfoRow("总内存", "${s.memTotalMb} MB")
                    InfoRow("可用", "${s.memAvailableMb} MB")
                    InfoRow("已用", "${s.memTotalMb - s.memAvailableMb} MB")
                }
            }
            // 电池
            InfoSectionCard(title = stringResource(R.string.monitor_battery), icon = Icons.Filled.BatteryChargingFull) {
                if (s == null) {
                    Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    UsageBar(label = "电量", percent = s.batteryLevel.toFloat())
                    InfoRow(stringResource(R.string.monitor_temperature), "%.1f °C".format(s.batteryTempC))
                    val cur = s.batteryCurrentMa
                    val curText = if (cur >= 0) "放电 $cur mA" else "充电 ${-cur} mA"
                    InfoRow("瞬时电流", curText)
                }
            }
        }
    }
}
