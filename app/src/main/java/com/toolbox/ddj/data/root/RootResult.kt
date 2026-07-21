package com.toolbox.ddj.data.root

import org.json.JSONArray
import org.json.JSONObject

/** 字段语义种类：决定 UI 如何渲染。 */
enum class FieldKind { TEXT, BOOL }

/** 单个标量字段：原始键、中文标签、显示值、语义种类。 */
data class ResultField(
    val key: String,
    val label: String,
    val value: String,
    val kind: FieldKind
)

/** 数组字段渲染成的表：中文标签 + 若干行，每行是一组字段。 */
data class ResultTable(
    val label: String,
    val rows: List<List<ResultField>>
)

/**
 * ROOT 脚本 JSON 输出的结构化结果。
 *
 * 脚本输出高度收敛为四形态：状态信封 {ok,msg/error}、键值对象、对象数组、纯文本。
 * 本模型把任意 JSON 拆成「状态 + 键值 + 数组 + reboot 标志」，供 RootResultView 数据驱动渲染；
 * 解析失败则 [parsedAsJson] = false，UI 回退等宽文本，保证不崩。
 */
data class RootResult(
    val ok: Boolean?,
    val message: String?,
    val reboot: Boolean,
    val fields: List<ResultField>,
    val tables: List<ResultTable>,
    val raw: String,
    val parsedAsJson: Boolean
) {
    /** 除状态横幅外是否有结构化内容可展示。 */
    val hasStructuredBody: Boolean get() = fields.isNotEmpty() || tables.isNotEmpty()

    companion object {
        /** 纯文本回退结果（非 JSON 或解析失败）。 */
        fun text(raw: String) = RootResult(
            ok = null, message = null, reboot = false,
            fields = emptyList(), tables = emptyList(),
            raw = raw, parsedAsJson = false
        )
    }
}

/**
 * JSON → [RootResult] 解析器。
 *
 * 只认对象（`{...}`）：脚本的数组都包在对象的某个键里（apps/packages/rules），
 * 裸数组极少见，一律回退文本，避免为不存在的情况增加分支。
 */
object RootResultParser {

    /** 已被信封语义单独消费、不再作为普通字段展示的键。 */
    private val ENVELOPE_KEYS = setOf("ok", "msg", "error", "reboot")

    fun parse(raw: String): RootResult {
        val t = raw.trim()
        if (t.isEmpty() || !t.startsWith("{")) return RootResult.text(raw)

        val obj = try {
            JSONObject(t)
        } catch (_: Exception) {
            return RootResult.text(raw)
        }

        // 状态横幅只认 ok；active 等布尔状态当普通字段展示，语义更准
        val ok: Boolean? = if (obj.has("ok")) obj.optBoolean("ok") else null
        val message: String? = obj.optString("msg")
            .ifBlank { obj.optString("error") }
            .ifBlank { null }
        val reboot = obj.optBoolean("reboot", false)

        val fields = mutableListOf<ResultField>()
        val tables = mutableListOf<ResultTable>()

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key in ENVELOPE_KEYS || obj.isNull(key)) continue
            when (val value = obj.get(key)) {
                is JSONArray -> parseArray(key, value)?.let { tables.add(it) }
                is Boolean -> fields.add(
                    ResultField(key, RootFieldLabels.label(key), value.toString(), FieldKind.BOOL)
                )
                is JSONObject -> {
                    // 嵌套对象少见，压平成一行文本，避免过度嵌套
                    fields.add(ResultField(key, RootFieldLabels.label(key), value.toString(), FieldKind.TEXT))
                }
                else -> {
                    val str = value.toString()
                    if (str.isNotBlank()) {
                        fields.add(ResultField(key, RootFieldLabels.label(key), str, FieldKind.TEXT))
                    }
                }
            }
        }

        return RootResult(
            ok = ok,
            message = message,
            reboot = reboot,
            fields = fields,
            tables = tables,
            raw = raw,
            parsedAsJson = true
        )
    }

    /** 数组字段 → 表；空数组返回 null（不生成空卡片）。 */
    private fun parseArray(key: String, arr: JSONArray): ResultTable? {
        if (arr.length() == 0) return null
        val rows = mutableListOf<List<ResultField>>()
        for (i in 0 until arr.length()) {
            when (val el = arr.get(i)) {
                is JSONObject -> {
                    val row = mutableListOf<ResultField>()
                    val ks = el.keys()
                    while (ks.hasNext()) {
                        val k = ks.next()
                        if (el.isNull(k)) continue
                        val v = el.get(k)
                        val kind = if (v is Boolean) FieldKind.BOOL else FieldKind.TEXT
                        row.add(ResultField(k, RootFieldLabels.label(k), v.toString(), kind))
                    }
                    if (row.isNotEmpty()) rows.add(row)
                }
                else -> rows.add(
                    listOf(ResultField(key, RootFieldLabels.label(key), el.toString(), FieldKind.TEXT))
                )
            }
        }
        return if (rows.isEmpty()) null else ResultTable(RootFieldLabels.label(key), rows)
    }
}

/**
 * 脚本 JSON 键名 → 中文标签词典。
 *
 * 覆盖已采集的高频键；未命中则把 snake_case 美化成 Title Case，保证任何键都有可读标签。
 */
object RootFieldLabels {

    private val dict: Map<String, String> = mapOf(
        // 通用 / 状态
        "current" to "当前值", "original" to "原始值", "orig" to "原始值",
        "modified" to "已修改", "count" to "数量", "total" to "总数",
        "state" to "状态", "pid" to "进程 PID", "running" to "运行中",
        "enabled" to "已启用", "active" to "已激活", "already" to "已在运行",
        "time" to "时间", "mode" to "模式", "version" to "版本",
        "updated" to "更新时间", "cached" to "已缓存",
        // 设备标识
        "android_id" to "Android ID", "oaid" to "OAID", "uuid" to "UUID",
        "sn" to "序列号", "serial" to "序列号",
        "ro_serialno" to "ro.serialno", "ro_boot_serialno" to "ro.boot.serialno",
        "ro_serial" to "ro.serial", "lenovo" to "联想标识", "oaid_file_ok" to "OAID 文件正常",
        // 机型 / 伪装
        "brand" to "品牌", "model" to "型号", "manufacturer" to "厂商",
        "id" to "预设 ID", "label" to "名称",
        "orig_brand" to "原始品牌", "orig_model" to "原始型号",
        // 触控
        "profile" to "档位", "daemon" to "守护进程",
        // 应用 / 列表
        "pkg" to "包名", "ssaid" to "SSAID", "ver" to "版本", "icon" to "图标",
        "packages" to "应用列表", "apps" to "应用列表", "rules" to "规则列表",
        "old" to "旧值", "new" to "新值", "action" to "动作",
        "key" to "键", "val" to "值", "value" to "值",
        // 防标记 / persist
        "force_hide" to "强制隐藏", "notify_always" to "总是通知",
        "auto_clean_ano" to "自动清理异常", "game" to "游戏",
        "mounted" to "已挂载数", "target" to "目标目录",
        "target_ok" to "目标正常", "target_items" to "目标项数",
        "persist_hide_enable" to "Persist 隐藏",
        // 模块设置
        "auto_clean_on_boot" to "开机自动清理",
        "auto_clean_on_codev_start" to "启动游戏时清理",
        "auto_clean_on_codev_stop" to "退出游戏时清理",
        "clean_mode" to "清理模式", "last_clean" to "上次清理",
        "current_spoof" to "当前伪装", "current_spoof_label" to "当前伪装机型",
        // 系统信息（sysinfo）
        "android" to "Android 版本", "sdk" to "SDK", "kernel" to "内核",
        "device" to "设备代号", "build_id" to "构建 ID", "build_date" to "构建日期",
        "cpu_abi" to "CPU ABI", "board" to "主板", "hardware" to "硬件",
        "security_patch" to "安全补丁", "fingerprint" to "指纹",
        "bootloader" to "Bootloader", "selinux" to "SELinux", "magisk" to "Root 方案",
        "screen" to "分辨率", "density" to "像素密度", "battery" to "电量",
        "uptime" to "运行时长(s)", "ram_mb" to "内存(MB)", "timezone" to "时区",
        "lang" to "语言",
        // MAC / 网络标识
        "iface" to "接口", "wifi_mac" to "WiFi MAC", "bt_mac" to "蓝牙 MAC",
        "orig_bt_mac" to "原始蓝牙 MAC", "mac" to "MAC", "wifi" to "WiFi", "bt" to "蓝牙"
    )

    fun label(key: String): String = dict[key] ?: prettify(key)

    private fun prettify(key: String): String =
        key.split('_').joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
