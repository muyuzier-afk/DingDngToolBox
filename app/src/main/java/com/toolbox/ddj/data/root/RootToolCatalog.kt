package com.toolbox.ddj.data.root

/**
 * ROOT 工具目录：把 shell 脚本能力映射成 UI 可操作的工具与动作。
 *
 * 脚本输出约定多为 JSON，UI 层直接展示原始结果，并做轻量解析。
 */
object RootToolCatalog {

    val tools: List<RootToolDef> = listOf(
        RootToolDef(
            id = "sysinfo",
            title = "系统信息 (Root)",
            description = "通过 getprop / dumpsys 采集完整设备信息",
            script = "sysinfo.sh",
            category = RootCategory.INFO,
            actions = listOf(
                RootActionDef("refresh", "刷新信息", emptyList())
            )
        ),
        RootToolDef(
            id = "android_id",
            title = "Android ID",
            description = "查看 / 随机 / 恢复系统 Android ID",
            script = "android_id.sh",
            category = RootCategory.IDENTITY,
            actions = listOf(
                RootActionDef("status", "查看状态", listOf("status")),
                RootActionDef("random", "随机生成", listOf("random"), needsConfirm = true),
                RootActionDef(
                    "set",
                    "自定义设置",
                    listOf("set"),
                    needsInput = true,
                    inputHint = "16 位十六进制 Android ID",
                    inputArgIndex = 1
                ),
                RootActionDef("restore", "恢复原始", listOf("restore"), needsConfirm = true)
            )
        ),
        RootToolDef(
            id = "ssaid",
            title = "SSAID 管理",
            description = "应用级 SSAID 查询与重置（游戏反作弊相关）",
            script = "ssaid.sh",
            category = RootCategory.IDENTITY,
            actions = listOf(
                RootActionDef("list", "常用游戏列表", listOf("list"), timeoutSec = 90),
                RootActionDef("rescan", "重新扫描应用", listOf("rescan")),
                RootActionDef("scan_status", "扫描状态", listOf("scan_status")),
                RootActionDef(
                    "get",
                    "查询指定包名",
                    listOf("get"),
                    needsInput = true,
                    inputHint = "包名 package name",
                    inputArgIndex = 1
                ),
                RootActionDef(
                    "reset",
                    "重置指定包名",
                    listOf("reset"),
                    needsInput = true,
                    inputHint = "包名 package name",
                    inputArgIndex = 1,
                    needsConfirm = true,
                    dangerous = true
                )
            )
        ),
        RootToolDef(
            id = "oaid",
            title = "OAID 管理",
            description = "OAID 查看、随机与重置（部分操作会重启）",
            script = "oaid.sh",
            category = RootCategory.IDENTITY,
            actions = listOf(
                RootActionDef("info", "查看信息", listOf("info")),
                RootActionDef("random_oaid", "随机 OAID", listOf("random_oaid"), needsConfirm = true),
                RootActionDef(
                    "wipe_partial",
                    "部分重置（会重启）",
                    listOf("wipe_partial"),
                    needsConfirm = true,
                    dangerous = true,
                    timeoutSec = 10
                ),
                RootActionDef(
                    "wipe_global",
                    "全局重置（会重启）",
                    listOf("wipe_global"),
                    needsConfirm = true,
                    dangerous = true,
                    timeoutSec = 10
                )
            )
        ),
        RootToolDef(
            id = "serial",
            title = "序列号管理",
            description = "通过 resetprop 修改 / 恢复设备序列号",
            script = "serial.sh",
            category = RootCategory.IDENTITY,
            actions = listOf(
                RootActionDef("info", "查看信息", listOf("info")),
                RootActionDef("random", "随机序列号", listOf("random"), needsConfirm = true),
                RootActionDef(
                    "apply",
                    "应用自定义 SN",
                    listOf("apply"),
                    needsInput = true,
                    inputHint = "序列号",
                    inputArgIndex = 1,
                    needsConfirm = true
                ),
                RootActionDef("restore", "恢复出厂 SN", listOf("restore"), needsConfirm = true)
            )
        ),
        RootToolDef(
            id = "spoof",
            title = "机型伪装",
            description = "应用 / 恢复机型预设（依赖本地 presets 缓存）",
            script = "spoof.sh",
            category = RootCategory.SPOOF,
            actions = listOf(
                RootActionDef("status", "当前状态", listOf("status")),
                RootActionDef(
                    "apply",
                    "应用预设 ID",
                    listOf("apply"),
                    needsInput = true,
                    inputHint = "预设 ID（见机型预设）",
                    inputArgIndex = 1,
                    needsConfirm = true
                ),
                RootActionDef("restore", "恢复原机型", listOf("restore"), needsConfirm = true)
            )
        ),
        RootToolDef(
            id = "presets",
            title = "机型预设",
            description = "导入 / 列出本地机型库",
            script = "presets.sh",
            category = RootCategory.SPOOF,
            actions = listOf(
                RootActionDef("list", "列出预设", listOf("list"), timeoutSec = 60),
                RootActionDef("info", "缓存信息", listOf("info")),
                RootActionDef(
                    "import",
                    "从文件导入",
                    listOf("import"),
                    needsInput = true,
                    inputHint = "设备上 JSON 路径",
                    inputArgIndex = 1
                )
            )
        ),
        RootToolDef(
            id = "anti_mark",
            title = "防设备标记",
            description = "安全版 persist 特征屏蔽与监控",
            script = "anti-mark.sh",
            category = RootCategory.PRIVACY,
            actions = listOf(
                RootActionDef("status", "状态", listOf("status")),
                RootActionDef("start", "启动", listOf("start"), needsConfirm = true),
                RootActionDef("stop", "停止", listOf("stop")),
                RootActionDef("get_config", "查看配置", listOf("get_config")),
                RootActionDef("log", "最近日志", listOf("log"))
            )
        ),
        RootToolDef(
            id = "persist_hide",
            title = "Persist 隐藏",
            description = "persist 分区游戏特征隐藏",
            script = "persist-hide.sh",
            category = RootCategory.PRIVACY,
            actions = listOf(
                RootActionDef("status", "状态", listOf("status")),
                RootActionDef("list", "列表", listOf("list")),
                RootActionDef("start", "启动", listOf("start"), needsConfirm = true),
                RootActionDef("stop", "停止", listOf("stop")),
                RootActionDef("enable", "启用", listOf("enable")),
                RootActionDef("disable", "禁用", listOf("disable")),
                RootActionDef("restore", "恢复", listOf("restore"), needsConfirm = true),
                RootActionDef("get_config", "配置", listOf("get_config"))
            )
        ),
        RootToolDef(
            id = "app_hide",
            title = "应用隐藏/冻结",
            description = "触发器规则：指定 App 启动时隐藏或冻结目标",
            script = "app_hide.sh",
            category = RootCategory.PRIVACY,
            actions = listOf(
                RootActionDef("list_rules", "列出规则", listOf("list_rules")),
                RootActionDef("list_apps", "应用列表", listOf("list_apps"), timeoutSec = 60),
                RootActionDef("daemon_status", "守护状态", listOf("daemon_status")),
                RootActionDef("daemon_start", "启动守护", listOf("daemon_start"), needsConfirm = true),
                RootActionDef("daemon_stop", "停止守护", listOf("daemon_stop")),
                RootActionDef("restore_all", "全部恢复", listOf("restore_all"), needsConfirm = true)
            )
        ),
        RootToolDef(
            id = "cleaner",
            title = "游戏缓存清理",
            description = "无畏契约 / 和平精英缓存清理与 inotify 优化",
            script = "cleaner.sh",
            category = RootCategory.CLEAN,
            actions = listOf(
                RootActionDef("deep", "深度清理（无畏）", listOf("deep"), timeoutSec = 120, needsConfirm = true),
                RootActionDef("light", "轻度清理（无畏）", listOf("light"), timeoutSec = 90, needsConfirm = true),
                RootActionDef("pubgmhd_deep", "深度清理（吃鸡）", listOf("pubgmhd_deep"), timeoutSec = 120, needsConfirm = true),
                RootActionDef("pubgmhd_light", "轻度清理（吃鸡）", listOf("pubgmhd_light"), timeoutSec = 90, needsConfirm = true),
                RootActionDef("inotify", "优化 inotify", listOf("inotify"))
            )
        ),
        RootToolDef(
            id = "monitor",
            title = "自动清理监听",
            description = "监听游戏启停并自动触发清理",
            script = "monitor.sh",
            category = RootCategory.CLEAN,
            actions = listOf(
                RootActionDef("status", "状态", listOf("status")),
                RootActionDef("start", "启动监听", listOf("start"), needsConfirm = true),
                RootActionDef("stop", "停止监听", listOf("stop")),
                RootActionDef("restart", "重启监听", listOf("restart"), needsConfirm = true)
            )
        ),
        RootToolDef(
            id = "touch_rate",
            title = "触控采样率",
            description = "提升触控采样档位并保持守护（机型相关 HIDL 命令）",
            script = "touch_rate.sh",
            category = RootCategory.TOUCH,
            actions = listOf(
                RootActionDef("status", "当前状态", listOf("status")),
                RootActionDef("set_125", "125Hz", listOf("set", "125"), needsConfirm = true),
                RootActionDef("set_240", "240Hz", listOf("set", "240"), needsConfirm = true),
                RootActionDef("set_360", "360Hz", listOf("set", "360"), needsConfirm = true),
                RootActionDef("set_600", "600Hz", listOf("set", "600"), needsConfirm = true),
                RootActionDef("set_default", "恢复默认", listOf("set", "default")),
                RootActionDef("stop", "停止守护", listOf("stop"))
            )
        ),
        RootToolDef(
            id = "settings",
            title = "模块设置",
            description = "开机清理、清理模式等开关",
            script = "settings.sh",
            category = RootCategory.SETTINGS,
            actions = listOf(
                RootActionDef("get", "读取设置", listOf("get")),
                RootActionDef(
                    "set_boot",
                    "开机清理 开/关 (1/0)",
                    listOf("set", "auto_clean_on_boot"),
                    needsInput = true,
                    inputHint = "1 或 0",
                    inputArgIndex = 2
                ),
                RootActionDef(
                    "set_mode",
                    "清理模式 light/deep",
                    listOf("set", "clean_mode"),
                    needsInput = true,
                    inputHint = "light 或 deep",
                    inputArgIndex = 2
                )
            )
        )
    )

    fun byId(id: String): RootToolDef? = tools.find { it.id == id }

    fun byCategory(): Map<RootCategory, List<RootToolDef>> =
        tools.groupBy { it.category }
}

enum class RootCategory(val label: String) {
    INFO("信息"),
    IDENTITY("设备标识"),
    SPOOF("机型伪装"),
    PRIVACY("隐私防护"),
    CLEAN("清理监控"),
    TOUCH("触控"),
    SETTINGS("设置")
}

data class RootToolDef(
    val id: String,
    val title: String,
    val description: String,
    val script: String,
    val category: RootCategory,
    val actions: List<RootActionDef>
)

/**
 * @param fixedArgs 固定参数前缀（不含用户输入）
 * @param inputArgIndex 用户输入插入到 args 的位置（从 0 计，含 fixed 合并后）
 *   实际实现：args = fixedArgs.toMutableList().apply { add(inputArgIndex.coerceAtMost(size), input) }
 *   更简单：fixedArgs + 若 needsInput 则追加 input
 */
data class RootActionDef(
    val id: String,
    val label: String,
    val fixedArgs: List<String>,
    val needsConfirm: Boolean = false,
    val needsInput: Boolean = false,
    val inputHint: String = "",
    val inputArgIndex: Int = -1,
    val dangerous: Boolean = false,
    val timeoutSec: Long = 45
) {
    fun buildArgs(userInput: String?): List<String> {
        if (!needsInput || userInput == null) return fixedArgs
        if (inputArgIndex < 0 || inputArgIndex >= fixedArgs.size) {
            return fixedArgs + userInput
        }
        val list = fixedArgs.toMutableList()
        // 在指定位置之后插入：例如 ["set"] + index 1 -> ["set", input]
        // 若 index == size，等价于 append
        val insertAt = inputArgIndex.coerceIn(0, list.size)
        // 当 fixed 为 ["set"] 且 index=1，insertAt=1 append
        // 当 fixed 为 ["set", "auto_clean_on_boot"] 且 index=2，append val
        if (insertAt >= list.size) {
            list.add(userInput)
        } else {
            list.add(insertAt, userInput)
        }
        return list
    }
}
