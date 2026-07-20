package com.toolbox.ddj.data

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 存储空间信息数据模型。
 *
 * 单位均为字节（Byte）。外部存储不可用时相关字段填 0。
 */
data class StorageInfo(
    val internalTotal: Long,
    val internalAvail: Long,
    val externalTotal: Long,
    val externalAvail: Long
)

/**
 * 存储空间采集 Repository。
 *
 * 通过 [StatFs] 读取内部（data 分区）与外部存储的总容量 / 可用容量，
 * 全部为公开 API，无需任何特殊权限。
 */
class StorageRepository {

    suspend fun get(): StorageInfo = withContext(Dispatchers.IO) {
        // 内部存储：data 目录所在分区
        val internal = statOf(Environment.getDataDirectory().path)

        // 外部存储：取不到或异常时以 0 兜底
        val external = runCatching {
            statOf(Environment.getExternalStorageDirectory().path)
        }.getOrDefault(0L to 0L)

        StorageInfo(
            internalTotal = internal.first,
            internalAvail = internal.second,
            externalTotal = external.first,
            externalAvail = external.second
        )
    }

    /**
     * 读取指定路径所在分区的 (总容量, 可用容量)，单位字节。
     */
    private fun statOf(path: String): Pair<Long, Long> {
        val stat = StatFs(path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val avail = stat.availableBlocksLong * stat.blockSizeLong
        return total to avail
    }
}
