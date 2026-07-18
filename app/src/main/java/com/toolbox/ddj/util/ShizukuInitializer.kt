package com.toolbox.ddj.util

import android.content.Context
import rikka.shizuku.Shizuku

/**
 * Shizuku 早期初始化钩子。
 *
 * Shizuku API 通过 ContentProvider 自动完成 binder 绑定（见 Manifest 中的
 * ShizukuProvider）。本类仅做：
 *  - 标记应用上下文（供后续封装使用）。
 *  - 注册 binder 死亡监听占位（Alpha 版暂不处理重连）。
 */
object ShizukuInitializer {

    @Volatile
    private var prepared = false

    fun prepare(context: Context) {
        if (prepared) return
        prepared = true
        // ShizukuProvider 已在 Application.onCreate 之前被系统初始化，
        // 此处仅注册 binder 状态监听用于将来重连，Alpha 版保持空实现。
        try {
            Shizuku.addBinderReceivedListenerSticky { /* binder 就绪 */ }
            Shizuku.addBinderDeadListener { /* binder 死亡 */ }
        } catch (e: Throwable) {
            // 早期设备/未安装 Shizuku 时静默忽略
        }
    }
}
