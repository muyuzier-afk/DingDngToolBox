package com.toolbox.ddj.ui.screens.screentools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toolbox.ddj.data.StorageInfo
import com.toolbox.ddj.data.StorageRepository
import com.toolbox.ddj.ui.components.InfoRow
import com.toolbox.ddj.ui.components.InfoSectionCard
import com.toolbox.ddj.ui.components.UsageBar

/**
 * 屏幕 & 存储工具页。
 *
 * 顶层用一个全屏 [Box] 承载：
 * - 主界面（存储信息 + 两个工具入口）
 * - 坏点检测全屏覆盖层
 * - 触摸测试全屏覆盖层
 * 通过 [ScreenToolsViewModel.uiState] 的 subMode 决定当前展示哪个。
 */
@Composable
fun ScreenToolsScreen(
    onBack: () -> Unit,
    viewModel: ScreenToolsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ScreenToolsViewModel(StorageRepository()) }
        }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (state.subMode) {
            SubMode.NONE -> MainContent(
                storage = state.storage,
                onBack = onBack,
                onDeadPixel = { viewModel.enter(SubMode.DEAD_PIXEL) },
                onTouchTest = { viewModel.enter(SubMode.TOUCH) }
            )

            SubMode.DEAD_PIXEL -> DeadPixelOverlay(onExit = { viewModel.exit() })
            SubMode.TOUCH -> TouchTestOverlay(onExit = { viewModel.exit() })
        }
    }
}

/**
 * 主界面：存储空间卡片 + 坏点检测 / 触摸测试入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    storage: StorageInfo?,
    onBack: () -> Unit,
    onDeadPixel: () -> Unit,
    onTouchTest: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("屏幕 & 存储") },
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
            StorageSection(storage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onDeadPixel,
                    modifier = Modifier.weight(1f)
                ) { Text("坏点检测") }
                FilledTonalButton(
                    onClick = onTouchTest,
                    modifier = Modifier.weight(1f)
                ) { Text("触摸测试") }
            }
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

/**
 * 存储空间信息卡片：内部存储占用条 + 总容量 / 可用容量；外部存储可用时同理展示。
 */
@Composable
private fun StorageSection(storage: StorageInfo?) {
    InfoSectionCard(title = "存储空间", icon = Icons.Filled.Storage) {
        if (storage == null) {
            InfoRow("存储信息", "加载中…")
            return@InfoSectionCard
        }

        // 内部存储
        UsageBar("内部存储", usedPercent(storage.internalTotal, storage.internalAvail))
        Spacer(Modifier.padding(top = 6.dp))
        InfoRow("内部总容量", "%.1f GB".format(bytesToGb(storage.internalTotal)))
        InfoRow("内部可用", "%.1f GB".format(bytesToGb(storage.internalAvail)))

        // 外部存储（取不到时 externalTotal 为 0，直接隐藏）
        if (storage.externalTotal > 0) {
            Spacer(Modifier.padding(top = 10.dp))
            UsageBar("外部存储", usedPercent(storage.externalTotal, storage.externalAvail))
            Spacer(Modifier.padding(top = 6.dp))
            InfoRow("外部总容量", "%.1f GB".format(bytesToGb(storage.externalTotal)))
            InfoRow("外部可用", "%.1f GB".format(bytesToGb(storage.externalAvail)))
        }
    }
}

/**
 * 坏点检测覆盖层：全屏纯色，点击循环切换颜色，返回键退出。
 */
@Composable
private fun DeadPixelOverlay(onExit: () -> Unit) {
    // 用于坏点排查的循环纯色列表
    val colors = remember {
        listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black, Color.Gray)
    }
    var index by remember { mutableIntStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }

    BackHandler { onExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors[index])
            .clickable(
                interactionSource = interactionSource,
                indication = null // 无涟漪，避免干扰坏点观察
            ) { index = (index + 1) % colors.size }
    ) {
        Text(
            text = "点击切换颜色，返回键退出",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(
                    Color.Black.copy(alpha = 0.45f),
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 触摸测试覆盖层：全屏黑底，跟踪多指按下位置并绘制彩色圆 + 坐标，返回键退出。
 */
@Composable
private fun TouchTestOverlay(onExit: () -> Unit) {
    // 当前处于按下状态的所有触点位置
    val points = remember { mutableStateListOf<Offset>() }
    // 多指区分用的配色
    val palette = remember {
        listOf(Color.Red, Color.Green, Color.Cyan, Color.Yellow, Color.Magenta, Color(0xFFFF9800))
    }
    // 坐标文字画笔（复用，避免重复创建）
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            isAntiAlias = true
        }
    }

    BackHandler { onExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        // 收集当前仍按下的指针位置，实时刷新
                        val pressed = event.changes.filter { it.pressed }.map { it.position }
                        points.clear()
                        points.addAll(pressed)
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            points.forEachIndexed { i, offset ->
                val color = palette[i % palette.size]
                drawCircle(color = color, radius = 60f, center = offset)
                // 在触点旁绘制像素坐标
                drawContext.canvas.nativeCanvas.drawText(
                    "(${offset.x.toInt()}, ${offset.y.toInt()})",
                    offset.x + 72f,
                    offset.y,
                    textPaint
                )
            }
        }
        Text(
            text = "多点触摸屏幕，返回键退出",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(
                    Color.White.copy(alpha = 0.15f),
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** 字节转 GB（1 GB = 1024^3 字节）。 */
private fun bytesToGb(bytes: Long): Double = bytes / (1024.0 * 1024.0 * 1024.0)

/** 计算占用百分比（0f~100f），total 为 0 时返回 0f 避免除零。 */
private fun usedPercent(total: Long, avail: Long): Float =
    if (total > 0) (total - avail) * 100f / total else 0f
