package com.toolbox.ddj.util

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 绑定 / 状态查询工具。
 *
 * Shizuku 是一个允许应用以 shell 或 root 权限运行代码的框架，
 * 在 Android 10+ 上无需 root 即可获得高于普通应用的权限（需用户预先启动 Shizuku）。
 *
 * 关于权限：调用 [Shizuku.checkSelfPermission] 在未绑定/未授权时会抛 DeadObjectException，
 * 因此包一层 try-catch。
 */
object ShizukuUtils {

    /** Shizuku 进程是否已运行（即 Shizuku App 已被启动）。 */
    fun isShizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    /** 设备是否已安装 Shizuku App。 */
    fun isShizukuInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Shizuku 是否可用 = 进程在跑 且 已被本应用授权。
     * Alpha 版仅做状态展示，不做请求授权弹窗的细化流程。
     */
    fun isShizukuGranted(): Boolean = try {
        isShizukuRunning() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    /**
     * 请求授权。Shizuku 在未授权时会触发系统授权弹窗。
     * Alpha 版预留入口，UI 暂不直接调用。
     */
    fun requestPermission(listener: Shizuku.OnRequestPermissionResultListener) {
        if (isShizukuRunning() && Shizuku.shouldShowRequestPermissionRationale()) {
            // 用户此前拒绝过，需自行引导
            return
        }
        Shizuku.requestPermission(0)
        Shizuku.addOnRequestPermissionResultListener(listener)
    }
}
