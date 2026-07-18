package com.toolbox.ddj

import android.app.Application
import com.toolbox.ddj.util.ShizukuInitializer

/**
 * 叮咚鸡工具箱 Application 入口。
 *
 * 负责：
 * - Shizuku 绑定的早期初始化（实际绑定在 Activity 中触发，此处仅做日志/状态准备）。
 */
class DingDongJiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        ShizukuInitializer.prepare(this)
    }

    companion object {
        @Volatile
        lateinit var instance: DingDongJiApp
            private set
    }
}
