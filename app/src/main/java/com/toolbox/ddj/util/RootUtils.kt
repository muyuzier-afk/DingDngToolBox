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
    fun isSuAvailable(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("which", "su"))
            if (!p.waitFor(2, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                return false
            }
            p.inputStream.bufferedReader().readText().isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 尝试执行 `su -c id`，返回是否成功提权。
     * 若弹授权框被拒绝/超时，均视为不可用。
     */
    fun isRootGranted(): Boolean {
        return try {
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

    /**
     * 以 root 执行一条 shell 命令，并捕获 stdout / stderr。
     *
     * @param command 传给 `su -c` 的完整命令字符串（注意自行转义）
     * @param timeoutSec 超时秒数，超时会 destroy 进程
     */
    fun execSu(command: String, timeoutSec: Long = 30): ShellResult {
        return try {
            if (!isSuAvailable()) {
                return ShellResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "su 不可用",
                    timedOut = false
                )
            }
            // 统一补全 PATH：部分定制 / 精简 ROM 的 su 子 shell PATH 缺失 /system/bin 等标准目录，
            // 会让 cp、chmod 等 toybox applet 报 "inaccessible or not found"。
            // 在此对所有经 execSu 执行的命令统一前置，一处加固、全局受益。
            val fullCommand =
                "export PATH=/system/bin:/system/xbin:/vendor/bin:/sbin:\$PATH; $command"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", fullCommand))
            val timedOut = !process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (timedOut) {
                process.destroyForcibly()
                return ShellResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "命令超时（${timeoutSec}s）",
                    timedOut = true
                )
            }
            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }
            ShellResult(
                exitCode = process.exitValue(),
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                timedOut = false
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: e.javaClass.simpleName,
                timedOut = false
            )
        }
    }

    /**
     * 以 root 执行脚本文件：`sh <scriptPath> [args...]`。
     * 参数会做基础 shell 单引号转义。
     */
    fun execScript(
        scriptPath: String,
        vararg args: String,
        timeoutSec: Long = 45
    ): ShellResult {
        val quotedArgs = args.joinToString(" ") { shellQuote(it) }
        val cmd = buildString {
            append("sh ")
            append(shellQuote(scriptPath))
            if (quotedArgs.isNotEmpty()) {
                append(' ')
                append(quotedArgs)
            }
        }
        return execSu(cmd, timeoutSec)
    }

    /** 将字符串包进单引号，内部单引号按 POSIX 规则转义。 */
    fun shellQuote(value: String): String {
        if (value.isEmpty()) return "''"
        return "'" + value.replace("'", "'\\''") + "'"
    }
}

/** Root shell 执行结果。 */
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false
) {
    val isSuccess: Boolean get() = !timedOut && exitCode == 0

    /** 优先 stdout；若为空则回退 stderr，便于 UI 展示。 */
    fun displayText(): String {
        return when {
            stdout.isNotBlank() -> stdout
            stderr.isNotBlank() -> stderr
            timedOut -> "超时"
            exitCode != 0 -> "退出码 $exitCode"
            else -> "(无输出)"
        }
    }
}
