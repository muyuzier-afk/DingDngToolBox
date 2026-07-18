package com.toolbox.ddj.ui.screens.deviceinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toolbox.ddj.DingDongJiApp
import com.toolbox.ddj.R
import com.toolbox.ddj.data.DeviceInfoRepository
import com.toolbox.ddj.ui.components.InfoRow
import com.toolbox.ddj.ui.components.InfoSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onBack: () -> Unit,
    viewModel: DeviceInfoViewModel = viewModel(
        factory = viewModelFactory { initializer { DeviceInfoViewModel(DeviceInfoRepository(DingDongJiApp.instance)) } }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tool_device_info)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.action_refresh))
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            DeviceInfoUiState.Loading -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.loading),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            is DeviceInfoUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { Text("加载失败：${s.message}") }
            is DeviceInfoUiState.Success -> DeviceInfoContent(s.data, padding)
        }
    }
}

@Composable
private fun DeviceInfoContent(
    data: com.toolbox.ddj.data.model.DeviceInfo,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoSectionCard(title = stringResource(R.string.info_section_hardware), icon = Icons.Filled.Smartphone) {
            InfoRow("品牌", data.hardware.brand)
            InfoRow("厂商", data.hardware.manufacturer)
            InfoRow("型号", data.hardware.model)
            InfoRow("设备代号", data.hardware.device)
            InfoRow("主板", data.hardware.board)
            InfoRow("硬件标识", data.hardware.hardware)
            InfoRow("支持 ABI", data.hardware.abis.joinToString(", "))
        }
        InfoSectionCard(title = stringResource(R.string.info_section_system), icon = Icons.Filled.Settings) {
            InfoRow("Android 版本", data.system.androidVersion)
            InfoRow("SDK 版本", data.system.sdkInt.toString())
            InfoRow("Build ID", data.system.buildId)
            InfoRow("安全补丁", data.system.securityPatch)
            InfoRow("Bootloader", data.system.bootloader)
            InfoRow("构建时间", data.system.buildTime)
            InfoRow("Fingerprint", data.system.fingerprint)
        }
        InfoSectionCard(title = stringResource(R.string.info_section_display), icon = Icons.Filled.Devices) {
            InfoRow("分辨率", "${data.display.widthPx} × ${data.display.heightPx} px")
            InfoRow("密度", "${data.display.densityDpi} dpi (${data.display.density}x)")
            InfoRow("DP 尺寸", "%.1f × %.1f dp".format(data.display.widthDp, data.display.heightDp))
            InfoRow("刷新率", "%.1f Hz".format(data.display.refreshRateHz))
        }
        InfoSectionCard(title = stringResource(R.string.info_section_battery), icon = Icons.Filled.BatteryFull) {
            InfoRow("电量", if (data.battery.level >= 0) "${data.battery.level}%" else "未知")
            InfoRow("状态", data.battery.status)
            InfoRow("健康度", data.battery.health)
            InfoRow("电源", data.battery.plugged)
            InfoRow("技术", data.battery.technology)
            InfoRow("温度", "%.1f °C".format(data.battery.temperatureC))
            InfoRow("电压", "${data.battery.voltageMv} mV")
        }
        // 占位：提示后续可结合 Root/Shizuku 读取更深层信息
        InfoSectionCard(title = "扩展（预览）", icon = Icons.Filled.Memory) {
            InfoRow("Root", "可于首页查看状态")
            InfoRow("Shizuku", "可于首页查看状态")
            InfoRow("说明", "Alpha 版仅采集公开 API 数据")
        }
    }
}
