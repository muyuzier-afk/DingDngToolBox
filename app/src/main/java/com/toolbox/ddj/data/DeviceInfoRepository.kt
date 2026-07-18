package com.toolbox.ddj.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.toolbox.ddj.data.model.BatteryInfo
import com.toolbox.ddj.data.model.DeviceInfo
import com.toolbox.ddj.data.model.DisplayInfo
import com.toolbox.ddj.data.model.HardwareInfo
import com.toolbox.ddj.data.model.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设备信息采集 Repository。
 *
 * 全部通过公开 API 获取，无需任何特殊权限。
 * - 硬件/系统：[Build] 静态字段
 * - 屏幕：[WindowManager] + [DisplayMetrics]
 * - 电池：sticky broadcast [Intent.ACTION_BATTERY_CHANGED]
 */
class DeviceInfoRepository(private val context: Context) {

    suspend fun collect(): DeviceInfo = withContext(Dispatchers.IO) {
        DeviceInfo(
            hardware = collectHardware(),
            system = collectSystem(),
            display = collectDisplay(),
            battery = collectBattery()
        )
    }

    private fun collectHardware(): HardwareInfo = HardwareInfo(
        brand = Build.BRAND,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        device = Build.DEVICE,
        board = Build.BOARD,
        hardware = Build.HARDWARE,
        abis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.toList()
        } else {
            listOf(Build.CPU_ABI)
        }
    )

    private fun collectSystem(): SystemInfo {
        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .format(Date(Build.TIME))
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            "unknown"
        }
        return SystemInfo(
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            buildId = Build.ID,
            fingerprint = Build.FINGERPRINT,
            securityPatch = securityPatch,
            bootloader = Build.BOOTLOADER,
            buildTime = buildTime
        )
    }

    private fun collectDisplay(): DisplayInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        return DisplayInfo(
            widthPx = metrics.widthPixels,
            heightPx = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            density = metrics.density,
            refreshRateHz = display.refreshRate,
            widthDp = metrics.widthPixels / metrics.density,
            heightDp = metrics.heightPixels / metrics.density
        )
    }

    private fun collectBattery(): BatteryInfo {
        // ACTION_BATTERY_CHANGED 是 sticky broadcast，立即返回最后一次状态
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (intent != null) parseBattery(intent) else BatteryInfo(
            level = -1,
            status = "未知",
            health = "未知",
            technology = "未知",
            temperatureC = 0f,
            voltageMv = 0,
            plugged = "未知"
        )
    }

    private fun parseBattery(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else -1

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            else -> "未知"
        }
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
            BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            else -> "未知"
        }
        val plugged = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC 电源"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线"
            else -> "未连接"
        }
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
        val volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "未知"

        return BatteryInfo(
            level = percent,
            status = status,
            health = health,
            technology = tech,
            temperatureC = temp,
            voltageMv = volt,
            plugged = plugged
        )
    }
}
