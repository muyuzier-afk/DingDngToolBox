package com.toolbox.ddj.ui.screens.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    LaunchedEffect(Unit) { viewModel.start() }

    val sample by viewModel.sample.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tool_system_monitor)) },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (active) viewModel.stop() else viewModel.start() },
                icon = {
                    Icon(
                        if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        if (active) stringResource(R.string.action_pause)
                        else stringResource(R.string.action_start)
                    )
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val s = sample
            InfoSectionCard(
                title = stringResource(R.string.monitor_cpu),
                icon = Icons.Filled.Speed
            ) {
                if (s == null) {
                    Text(
                        stringResource(R.string.loading),
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    UsageBar(
                        label = stringResource(R.string.monitor_usage),
                        percent = s.cpuUsagePercent
                    )
                    InfoRow(
                        stringResource(R.string.monitor_cores),
                        s.coreFreqMHz.size.toString()
                    )
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

            InfoSectionCard(
                title = stringResource(R.string.monitor_memory),
                icon = Icons.Filled.Memory
            ) {
                if (s == null) {
                    Text(
                        stringResource(R.string.loading),
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    UsageBar(
                        label = stringResource(R.string.monitor_usage),
                        percent = s.memUsagePercent
                    )
                    InfoRow(stringResource(R.string.monitor_mem_total), "${s.memTotalMb} MB")
                    InfoRow(stringResource(R.string.monitor_mem_available), "${s.memAvailableMb} MB")
                    InfoRow(
                        stringResource(R.string.monitor_mem_used),
                        "${s.memTotalMb - s.memAvailableMb} MB"
                    )
                }
            }

            InfoSectionCard(
                title = stringResource(R.string.monitor_battery),
                icon = Icons.Filled.BatteryChargingFull
            ) {
                if (s == null) {
                    Text(
                        stringResource(R.string.loading),
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    UsageBar(
                        label = stringResource(R.string.info_level),
                        percent = s.batteryLevel.toFloat()
                    )
                    InfoRow(
                        stringResource(R.string.monitor_temperature),
                        "%.1f °C".format(s.batteryTempC)
                    )
                    val cur = s.batteryCurrentMa
                    val curText = if (cur >= 0) {
                        stringResource(R.string.monitor_discharge, cur)
                    } else {
                        stringResource(R.string.monitor_charge, -cur)
                    }
                    InfoRow(stringResource(R.string.monitor_current), curText)
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 88.dp))
        }
    }
}
