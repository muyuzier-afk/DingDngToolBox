package com.toolbox.ddj.util

import java.util.concurrent.TimeUnit

/**
 * Root 权限检测与执行工具。
 *
 * 实现策略：通过 `which su` + 执行 `su -c id` 校验是否真正获得 root。
 * 不缓存结果，由调用方按需调用（每次校验开销极低）。
 */
object RootUtils {

    /** 是否检测到 su 二进制可用。 */
    fun isSuAvailable(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("which", "su"))
        // kotlin.Process.waitFor(timeout, unit) 扩展返回 Boolean
        if (!p.waitFor(2, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            return false
        }
        p.inputStream.bufferedReader().readText().isNotBlank()
    } catch (e: Exception) {
        false
    }

    /**
     * 尝试执行 `su -c id`，返回是否成功提权。
     * 若弹授权框被拒绝/超时，均视为不可用。
     */
    fun isRootGranted(): Boolean = try {
        if (!isSuAvailable()) return false
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        if (!p.waitFor(3, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            return false
        }
        p.exitValue() == 0
    } catch (e: Exception) {
        false
    }
}
