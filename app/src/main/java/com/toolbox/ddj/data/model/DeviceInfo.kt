package com.toolbox.ddj.data.model

/**
 * 设备信息聚合模型，按 [section] 分组显示。
 */
data class DeviceInfo(
    val hardware: HardwareInfo,
    val system: SystemInfo,
    val display: DisplayInfo,
    val battery: BatteryInfo
)

data class HardwareInfo(
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val board: String,
    val hardware: String,
    val abis: List<String>
)

data class SystemInfo(
    val androidVersion: String,
    val sdkInt: Int,
    val buildId: String,
    val fingerprint: String,
    val securityPatch: String,
    val bootloader: String,
    val buildTime: String
)

data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val density: Float,
    val refreshRateHz: Float,
    val widthDp: Float,
    val heightDp: Float
)

data class BatteryInfo(
    val level: Int,
    val status: String,
    val health: String,
    val technology: String,
    val temperatureC: Float,
    val voltageMv: Int,
    val plugged: String
)
