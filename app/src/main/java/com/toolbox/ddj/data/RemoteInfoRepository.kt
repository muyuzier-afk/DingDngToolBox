package com.toolbox.ddj.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** 云端拉取的版本 + 公告信息。 */
data class RemoteInfo(
    val versionCode: Int,
    val versionName: String,
    /** 已按系统语言选定的公告 Markdown 文本；拉取失败为空串。 */
    val announcement: String
)

/**
 * 从仓库 raw 拉取云端版本号与双语公告。
 *
 * 只读两个小 JSON，用原生 [HttpURLConnection]（不为此引入网络库）。
 * 任一步失败即返回 null / 空串，UI 回退本地文案，永不崩。
 */
class RemoteInfoRepository {

    companion object {
        private const val BASE =
            "https://raw.githubusercontent.com/muyuzier-afk/DingDngToolBox/main"
        private const val VERSION_URL = "$BASE/VERSION"
        private const val ANNOUNCE_URL = "$BASE/CLOUDANNOUNCEMENT.md"
        private const val TIMEOUT_MS = 5000
    }

    /** 拉取云端信息；版本不可用则整体返回 null，公告可选（缺失为空串）。 */
    suspend fun fetch(): RemoteInfo? = withContext(Dispatchers.IO) {
        val versionJson = httpGet(VERSION_URL) ?: return@withContext null
        try {
            val v = JSONObject(versionJson)
            val code = v.optInt("versionCode", -1)
            val name = v.optString("versionName")
            if (code < 0 || name.isBlank()) return@withContext null
            val announcement = httpGet(ANNOUNCE_URL)?.let { pickAnnouncement(it) }.orEmpty()
            RemoteInfo(versionCode = code, versionName = name, announcement = announcement)
        } catch (_: Exception) {
            null
        }
    }

    /** 按系统语言选公告：zh 开头取 zh-CN，否则 en-US；缺失回退另一个。 */
    private fun pickAnnouncement(json: String): String = try {
        val obj = JSONObject(json)
        val isZh = Locale.getDefault().language.startsWith("zh", ignoreCase = true)
        val primary = if (isZh) "zh-CN" else "en-US"
        val fallback = if (isZh) "en-US" else "zh-CN"
        obj.optString(primary).ifBlank { obj.optString(fallback) }
    } catch (_: Exception) {
        ""
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
