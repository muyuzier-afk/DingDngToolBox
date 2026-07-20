package com.toolbox.ddj.ui.screens.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
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
import com.toolbox.ddj.data.NetworkInfo
import com.toolbox.ddj.data.NetworkInfoRepository
import com.toolbox.ddj.ui.components.InfoRow
import com.toolbox.ddj.ui.components.InfoSectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInfoScreen(
    onBack: () -> Unit,
    viewModel: NetworkInfoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NetworkInfoViewModel(NetworkInfoRepository(DingDongJiApp.instance)) }
        }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("网络信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(com.toolbox.ddj.R.string.action_back)
                        )
                    }
                },
                actions = {
                    // 右上角刷新：重新采集网络信息
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "刷新")
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
        val info = state.info
        if (state.loading || info == null) {
            // 加载中：居中转圈
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NetworkInfoContent(info)
                Spacer(Modifier.padding(bottom = 24.dp))
            }
        }
    }
}

/** 三张分区卡片：连接状态 / IP 地址 / DNS 与网关 */
@Composable
private fun NetworkInfoContent(info: NetworkInfo) {
    InfoSectionCard(title = "连接状态", icon = Icons.Filled.Wifi) {
        InfoRow("连接类型", info.type)
        InfoRow("是否计费", info.metered)
        InfoRow("已验证", info.validated)
    }
    InfoSectionCard(title = "IP 地址", icon = Icons.Filled.Lan) {
        InfoRow("IPv4", info.ipv4)
        InfoRow("IPv6", info.ipv6)
    }
    InfoSectionCard(title = "DNS 与网关", icon = Icons.Filled.Dns) {
        InfoRow("DNS", info.dns)
        InfoRow("网关", info.gateway)
        InfoRow("接口", info.iface)
        InfoRow("MTU", info.mtu)
    }
}
