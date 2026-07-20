package com.toolbox.ddj.ui.screens.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.ddj.R
import com.toolbox.ddj.ui.components.StatusBanner
import com.toolbox.ddj.ui.components.TonalCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDeviceInfo: () -> Unit,
    onOpenSystemMonitor: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenSensors: () -> Unit,
    onOpenScreenTools: () -> Unit,
    onOpenRootTools: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val perm by viewModel.permState.collectAsStateWithLifecycle()
    val remote by viewModel.remote.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val anyElevated = perm.rootGranted || perm.shizukuGranted

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.refreshPermissionState() }) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnnouncementCard(announcement = remote?.announcement)

            PermissionStatusCard(
                rootGranted = perm.rootGranted,
                shizukuGranted = perm.shizukuGranted,
                shizukuInstalled = perm.shizukuInstalled,
                shizukuRunning = perm.shizukuRunning,
                elevated = anyElevated
            )

            VersionInfoCard(
                version = viewModel.versionName,
                latest = remote?.versionName,
                hasUpdate = viewModel.hasUpdate(remote)
            )

            SectionLabel(stringResource(R.string.home_section_basic))

            ToolEntryCard(
                title = stringResource(R.string.tool_device_info),
                description = stringResource(R.string.tool_device_info_desc),
                icon = Icons.Filled.Devices,
                onClick = onOpenDeviceInfo
            )
            ToolEntryCard(
                title = stringResource(R.string.tool_system_monitor),
                description = stringResource(R.string.tool_system_monitor_desc),
                icon = Icons.Filled.Speed,
                onClick = onOpenSystemMonitor
            )
            ToolEntryCard(
                title = stringResource(R.string.tool_apps),
                description = stringResource(R.string.tool_apps_desc),
                icon = Icons.Filled.Apps,
                onClick = onOpenApps
            )
            ToolEntryCard(
                title = stringResource(R.string.tool_network),
                description = stringResource(R.string.tool_network_desc),
                icon = Icons.Filled.Wifi,
                onClick = onOpenNetwork
            )
            ToolEntryCard(
                title = stringResource(R.string.tool_sensors),
                description = stringResource(R.string.tool_sensors_desc),
                icon = Icons.Filled.Sensors,
                onClick = onOpenSensors
            )
            ToolEntryCard(
                title = stringResource(R.string.tool_screentools),
                description = stringResource(R.string.tool_screentools_desc),
                icon = Icons.Filled.Storage,
                onClick = onOpenScreenTools
            )

            SectionLabel(stringResource(R.string.home_section_advanced))
            ToolEntryCard(
                title = stringResource(R.string.tool_root_tools),
                description = stringResource(R.string.tool_root_tools_desc),
                icon = Icons.Filled.AdminPanelSettings,
                onClick = onOpenRootTools
            )
            ToolEntryCard(
                title = stringResource(R.string.tool_more_coming),
                description = stringResource(R.string.tool_more_coming_desc),
                icon = Icons.Filled.Build,
                enabled = false,
                onClick = {}
            )

            Text(
                text = stringResource(R.string.home_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PermissionStatusCard(
    rootGranted: Boolean,
    shizukuGranted: Boolean,
    shizukuInstalled: Boolean,
    shizukuRunning: Boolean,
    elevated: Boolean
) {
    TonalCard(
        containerColor = if (elevated) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.perm_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (elevated) {
                            stringResource(R.string.perm_ready)
                        } else {
                            stringResource(R.string.perm_missing)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AssistChip(
                    onClick = {},
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (elevated) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        leadingIconContentColor = if (elevated) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    ),
                    label = {
                        Text(
                            if (elevated) stringResource(R.string.perm_granted)
                            else stringResource(R.string.perm_action_required)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (elevated) Icons.Filled.CheckCircle
                            else Icons.Filled.ErrorOutline,
                            contentDescription = null
                        )
                    }
                )
            }

            PermissionLine(
                label = stringResource(R.string.perm_root_status),
                available = rootGranted,
                icon = Icons.Filled.Build
            )
            val shizukuDetail = when {
                !shizukuInstalled -> stringResource(R.string.perm_shizuku_not_installed)
                !shizukuRunning -> stringResource(R.string.perm_shizuku_not_running)
                else -> null
            }
            PermissionLine(
                label = stringResource(R.string.perm_shizuku_status),
                available = shizukuGranted,
                detail = shizukuDetail,
                icon = Icons.Filled.Verified
            )
        }
    }
}

@Composable
private fun PermissionLine(
    label: String,
    available: Boolean,
    detail: String? = null,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (available) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        val statusText = detail
            ?: if (available) stringResource(R.string.perm_available)
            else stringResource(R.string.perm_unavailable)
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (available) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun AnnouncementCard(announcement: String?) {
    val text = announcement?.takeIf { it.isNotBlank() }
    if (text != null) {
        TonalCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.padding(20.dp)) {
                MarkdownLite(text, MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    } else {
        // 云端公告未拉到时回退本地预览提示
        StatusBanner(
            message = stringResource(R.string.home_preview_notice),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** 极简 Markdown：仅处理 # / ## / ### 标题行与空行，不为公告引 Markdown 库。 */
@Composable
private fun MarkdownLite(text: String, contentColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        text.trim().lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                line.startsWith("### ") -> Text(
                    line.removePrefix("### "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                line.startsWith("## ") -> Text(
                    line.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                line.startsWith("# ") -> Text(
                    line.removePrefix("# "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                else -> Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun VersionInfoCard(version: String, latest: String?, hasUpdate: Boolean) {
    TonalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_app_version),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = version,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_latest_version),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = latest ?: stringResource(R.string.home_checking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (latest != null) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (hasUpdate) stringResource(R.string.home_update_available)
                                else stringResource(R.string.home_up_to_date)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = if (hasUpdate) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TonalCard(
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
