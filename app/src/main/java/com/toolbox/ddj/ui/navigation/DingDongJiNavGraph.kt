package com.toolbox.ddj.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toolbox.ddj.ui.screens.deviceinfo.DeviceInfoScreen
import com.toolbox.ddj.ui.screens.home.HomeScreen
import com.toolbox.ddj.ui.screens.monitor.SystemMonitorScreen
import com.toolbox.ddj.ui.screens.root.RootToolDetailScreen
import com.toolbox.ddj.ui.screens.root.RootToolsScreen

/** 顶层路由。 */
object Routes {
    const val HOME = "home"
    const val DEVICE_INFO = "device_info"
    const val SYSTEM_MONITOR = "system_monitor"
    const val ROOT_TOOLS = "root_tools"
    const val ROOT_TOOL_DETAIL = "root_tool/{toolId}"

    fun rootToolDetail(toolId: String) = "root_tool/$toolId"
}

@Composable
fun DingDongJiNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenDeviceInfo = { nav.navigate(Routes.DEVICE_INFO) },
                onOpenSystemMonitor = { nav.navigate(Routes.SYSTEM_MONITOR) },
                onOpenRootTools = { nav.navigate(Routes.ROOT_TOOLS) }
            )
        }
        composable(Routes.DEVICE_INFO) {
            DeviceInfoScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SYSTEM_MONITOR) {
            SystemMonitorScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.ROOT_TOOLS) {
            RootToolsScreen(
                onBack = { nav.popBackStack() },
                onOpenTool = { toolId -> nav.navigate(Routes.rootToolDetail(toolId)) }
            )
        }
        composable(
            route = Routes.ROOT_TOOL_DETAIL,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) { entry ->
            val toolId = entry.arguments?.getString("toolId").orEmpty()
            RootToolDetailScreen(
                toolId = toolId,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
