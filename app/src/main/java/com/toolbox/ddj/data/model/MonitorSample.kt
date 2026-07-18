package com.toolbox.ddj.data.model

/**
 * 一次系统监控采样。
 *
 * - [cpuUsagePercent]：总体 CPU 占用百分比（0..100）。
 * - [coreFreqMHz]：各核心当前主频（MHz），长度 = 逻辑核心数。
 * - [memUsagePercent]：内存占用百分比。
 * - [memAvailableMb] / [memTotalMb]：可用 / 总内存。
 * - [batteryLevel]：0..100。
 * - [batteryTempC]：电池温度。
 * - [batteryCurrentMa]：瞬时电流（mA），正为放电负为充电。
 */
data class MonitorSample(
    val timestamp: Long,
    val cpuUsagePercent: Float,
    val coreFreqMHz: List<Int>,
    val memUsagePercent: Float,
    val memAvailableMb: Long,
    val memTotalMb: Long,
    val batteryLevel: Int,
    val batteryTempC: Float,
    val batteryCurrentMa: Float
)
