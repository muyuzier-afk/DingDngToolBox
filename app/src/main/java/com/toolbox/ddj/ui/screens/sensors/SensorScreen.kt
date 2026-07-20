package com.toolbox.ddj.ui.screens.sensors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import com.toolbox.ddj.data.SensorItem
import com.toolbox.ddj.data.SensorRepository
import com.toolbox.ddj.ui.components.InfoRow
import com.toolbox.ddj.ui.components.InfoSectionCard

/**
 * 传感器页：顶部展示主要传感器的实时读数，下方列出设备全部传感器的静态信息。
 *
 * 传感器数量较多，外层用 verticalScroll 统一滚动（与 DeviceInfoScreen 保持一致的模式）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
    onBack: () -> Unit,
    viewModel: SensorViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SensorViewModel(SensorRepository(DingDongJiApp.instance)) }
        }
    )
) {
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val sensors = viewModel.sensors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("传感器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(com.toolbox.ddj.R.string.action_back)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 实时读数区：遍历 Map 逐行显示，空时给出提示
            InfoSectionCard(title = "实时读数", icon = Icons.Filled.Speed) {
                if (readings.isEmpty()) {
                    Text(
                        text = "暂无实时数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    readings.forEach { (name, value) ->
                        InfoRow(name, value)
                    }
                }
            }

            // 全部传感器列表区
            InfoSectionCard(title = "全部传感器（${sensors.size}）", icon = Icons.Filled.Sensors) {
                sensors.forEach { item ->
                    SensorRowItem(item)
                }
            }

            // 底部留白，避免最后一项贴边
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

/** 单个传感器行：名称 titleSmall + 厂商 / 类型 / 量程等 bodySmall 次级色。 */
@Composable
private fun SensorRowItem(item: SensorItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "厂商：${item.vendor} · 类型：${item.typeLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "量程：${item.maxRange} · 分辨率：${item.resolution} · 功耗：${item.power}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
