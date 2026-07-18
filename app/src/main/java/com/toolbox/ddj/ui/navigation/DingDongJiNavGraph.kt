package com.toolbox.ddj.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toolbox.ddj.ui.screens.deviceinfo.DeviceInfoScreen
import com.toolbox.ddj.ui.screens.home.HomeScreen
import com.toolbox.ddj.ui.screens.monitor.SystemMonitorScreen

/** 顶层路由。 */
object Routes {
    const val HOME = "home"
    const val DEVICE_INFO = "device_info"
    const val SYSTEM_MONITOR = "system_monitor"
}

@Composable
fun DingDongJiNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenDeviceInfo = { nav.navigate(Routes.DEVICE_INFO) },
                onOpenSystemMonitor = { nav.navigate(Routes.SYSTEM_MONITOR) }
            )
        }
        composable(Routes.DEVICE_INFO) {
            DeviceInfoScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SYSTEM_MONITOR) {
            SystemMonitorScreen(onBack = { nav.popBackStack() })
        }
    }
}
