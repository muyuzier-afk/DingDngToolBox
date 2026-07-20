package com.toolbox.ddj.data

import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 单个已安装应用的信息快照。
 *
 * @param label 应用显示名称
 * @param packageName 包名（唯一标识）
 * @param versionName 版本名（如 1.2.3）
 * @param versionCode 版本号（长整型）
 * @param sizeBytes APK 体积（字节）
 * @param isSystem 是否为系统应用
 * @param firstInstall 首次安装时间戳（毫秒）
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeBytes: Long,
    val isSystem: Boolean,
    val firstInstall: Long
)

/**
 * 应用列表采集 Repository。
 *
 * 通过公开的 [android.content.pm.PackageManager] 读取全部已安装包，无需特殊权限。
 */
class AppListRepository(private val context: Context) {

    /**
     * 列出全部已安装应用，按名称（忽略大小写）升序排序。
     *
     * IO 密集操作，切到 [Dispatchers.IO] 执行。
     */
    suspend fun list(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledPackages(0).mapNotNull { pkg ->
            // 新版 SDK 将 applicationInfo 标注为可空，做兜底跳过异常包
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            AppInfo(
                label = appInfo.loadLabel(pm).toString(),
                packageName = pkg.packageName,
                versionName = pkg.versionName ?: "",
                versionCode = pkg.longVersionCode,
                sizeBytes = File(appInfo.sourceDir).length(),
                isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                firstInstall = pkg.firstInstallTime
            )
        }.sortedBy { it.label.lowercase() }
    }
}
