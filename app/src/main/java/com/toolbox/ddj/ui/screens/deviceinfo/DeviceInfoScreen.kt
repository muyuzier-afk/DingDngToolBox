package com.toolbox.ddj.ui.screens.deviceinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.toolbox.ddj.data.DeviceInfoRepository
import com.toolbox.ddj.data.model.DeviceInfo
import com.toolbox.ddj.ui.components.InfoRow
import com.toolbox.ddj.ui.components.InfoSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onBack: () -> Unit,
    viewModel: DeviceInfoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DeviceInfoViewModel(DeviceInfoRepository(DingDongJiApp.instance)) }
        }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tool_device_info)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.action_refresh))
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
        when (val s = state) {
            DeviceInfoUiState.Loading -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.loading),
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            is DeviceInfoUiState.Error -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.load_failed, s.message),
                    color = MaterialTheme.colorScheme.error
                )
            }

            is DeviceInfoUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DeviceInfoContent(s.data)
                SpacerVertical()
            }
        }
    }
}

@Composable
private fun SpacerVertical() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 24.dp))
}

@Composable
private fun DeviceInfoContent(data: DeviceInfo) {
    InfoSectionCard(
        title = stringResource(R.string.info_section_hardware),
        icon = Icons.Filled.Smartphone
    ) {
        InfoRow(stringResource(R.string.info_brand), data.hardware.brand)
        InfoRow(stringResource(R.string.info_manufacturer), data.hardware.manufacturer)
        InfoRow(stringResource(R.string.info_model), data.hardware.model)
        InfoRow(stringResource(R.string.info_device_code), data.hardware.device)
        InfoRow(stringResource(R.string.info_board), data.hardware.board)
        InfoRow(stringResource(R.string.info_hardware), data.hardware.hardware)
        InfoRow(stringResource(R.string.info_abis), data.hardware.abis.joinToString(", "))
    }
    InfoSectionCard(
        title = stringResource(R.string.info_section_system),
        icon = Icons.Filled.Settings
    ) {
        InfoRow(stringResource(R.string.info_android_version), data.system.androidVersion)
        InfoRow(stringResource(R.string.info_sdk), data.system.sdkInt.toString())
        InfoRow(stringResource(R.string.info_build_id), data.system.buildId)
        InfoRow(stringResource(R.string.info_security_patch), data.system.securityPatch)
        InfoRow(stringResource(R.string.info_bootloader), data.system.bootloader)
        InfoRow(stringResource(R.string.info_build_time), data.system.buildTime)
        InfoRow(stringResource(R.string.info_fingerprint), data.system.fingerprint)
    }
    InfoSectionCard(
        title = stringResource(R.string.info_section_display),
        icon = Icons.Filled.Devices
    ) {
        InfoRow(
            stringResource(R.string.info_resolution),
            "${data.display.widthPx} × ${data.display.heightPx} px"
        )
        InfoRow(
            stringResource(R.string.info_density),
            "${data.display.densityDpi} dpi (${data.display.density}x)"
        )
        InfoRow(
            stringResource(R.string.info_dp_size),
            "%.1f × %.1f dp".format(data.display.widthDp, data.display.heightDp)
        )
        InfoRow(
            stringResource(R.string.info_refresh_rate),
            "%.1f Hz".format(data.display.refreshRateHz)
        )
    }
    InfoSectionCard(
        title = stringResource(R.string.info_section_battery),
        icon = Icons.Filled.BatteryFull
    ) {
        InfoRow(
            stringResource(R.string.info_level),
            if (data.battery.level >= 0) "${data.battery.level}%" else stringResource(R.string.unknown)
        )
        InfoRow(stringResource(R.string.info_status), data.battery.status)
        InfoRow(stringResource(R.string.info_health), data.battery.health)
        InfoRow(stringResource(R.string.info_plugged), data.battery.plugged)
        InfoRow(stringResource(R.string.info_technology), data.battery.technology)
        InfoRow(
            stringResource(R.string.info_temperature),
            "%.1f °C".format(data.battery.temperatureC)
        )
        InfoRow(stringResource(R.string.info_voltage), "${data.battery.voltageMv} mV")
    }
    InfoSectionCard(
        title = stringResource(R.string.info_section_extended),
        icon = Icons.Filled.Memory
    ) {
        InfoRow(stringResource(R.string.perm_root_status), stringResource(R.string.info_check_home))
        InfoRow(stringResource(R.string.perm_shizuku_status), stringResource(R.string.info_check_home))
        InfoRow(stringResource(R.string.info_note_label), stringResource(R.string.info_note_alpha))
    }
}
