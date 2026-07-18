package com.toolbox.ddj.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.toolbox.ddj.data.model.MonitorSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * 系统监控 Repository。
 *
 * 通过周期性读取 /proc 与 /sys 实现免权限监控：
 * - CPU 总体占用：解析 /proc/stat 两次采样差值
 * - 每核心主频：/sys/devices/system/cpu/cpuN/cpufreq/scaling_cur_freq
 * - 内存：/proc/meminfo
 * - 电池：sticky broadcast + BatteryManager 电流 API（Android L+）
 *
 * [sample] 默认每 1.5s 采样一次，调用方可自由 collect / take。
 */
class SystemMonitorRepository(private val context: Context) {

    fun sample(intervalMs: Long = 1500L): Flow<MonitorSample> = flow {
        var prevCpu = readProcStat()
        while (true) {
            delay(intervalMs)
            val currCpu = readProcStat()
            val cpuUsage = computeCpuUsage(prevCpu, currCpu)
            prevCpu = currCpu

            val mem = readMemInfo()
            val battery = readBattery()

            emit(
                MonitorSample(
                    timestamp = System.currentTimeMillis(),
                    cpuUsagePercent = cpuUsage,
                    coreFreqMHz = readCoreFreqs(),
                    memUsagePercent = if (mem.total > 0) {
                        (1f - mem.available.toFloat() / mem.total) * 100f
                    } else 0f,
                    memAvailableMb = mem.available / 1024,
                    memTotalMb = mem.total / 1024,
                    batteryLevel = battery.level,
                    batteryTempC = battery.tempC,
                    batteryCurrentMa = battery.currentMa
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    // ---------- CPU ----------

    /** 返回 [user, nice, system, idle, iowait, irq, softirq, steal] 累计 jiffies 数组。 */
    private fun readProcStat(): LongArray {
        val line = try {
            File("/proc/stat").bufferedReader().readLine()
        } catch (e: Exception) {
            return LongArray(0)
        }
        if (!line.startsWith("cpu ")) return LongArray(0)
        return line.substring("cpu ".length).trim().split(Regex("\\s+"))
            .map { it.toLongOrNull() ?: 0L }
            .toLongArray()
    }

    private fun computeCpuUsage(prev: LongArray, curr: LongArray): Float {
        if (prev.size < 4 || curr.size < 4) return 0f
        val prevIdle = prev[3]
        val currIdle = curr[3]
        val prevTotal = prev.sum()
        val currTotal = curr.sum()
        val totalDelta = currTotal - prevTotal
        val idleDelta = currIdle - prevIdle
        if (totalDelta <= 0) return 0f
        return ((totalDelta - idleDelta).toFloat() / totalDelta) * 100f
    }

    private fun readCoreFreqs(): List<Int> {
        val freqs = mutableListOf<Int>()
        var i = 0
        while (true) {
            val f = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            if (!f.exists()) break
            val khz = f.readText().trim().toIntOrNull() ?: break
            freqs.add(khz / 1000)
            i++
        }
        // 部分设备 cpufreq 不可读，回退用逻辑核心数
        if (freqs.isEmpty()) {
            val cores = Runtime.getRuntime().availableProcessors()
            repeat(cores) { freqs.add(0) }
        }
        return freqs
    }

    // ---------- 内存 ----------

    private data class MemInfo(val total: Long, val available: Long)

    private fun readMemInfo(): MemInfo {
        val map = try {
            File("/proc/meminfo").bufferedReader().readLines()
                .mapNotNull {
                    val kv = it.split(":", limit = 2)
                    if (kv.size != 2) return@mapNotNull null
                    val key = kv[0].trim()
                    val value = kv[1].trim().split(Regex("\\s+"))[0].toLongOrNull() ?: 0L
                    key to value
                }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
        val total = map["MemTotal"] ?: 0L
        val available = map["MemAvailable"]
            ?: ((map["MemFree"] ?: 0L) + (map["Buffers"] ?: 0L) + (map["Cached"] ?: 0L))
        return MemInfo(total = total * 1024, available = available * 1024)
    }

    // ---------- 电池 ----------

    private data class BatterySnapshot(
        val level: Int,
        val tempC: Float,
        val currentMa: Float
    )

    private fun readBattery(): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.let {
            val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (l >= 0 && s > 0) (l * 100) / s else -1
        } ?: -1
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10f) ?: 0f
        val currentMa = try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            // 返回 μA，转为 mA
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000f
        } catch (e: Exception) {
            0f
        }
        return BatterySnapshot(level, temp, currentMa)
    }
}
