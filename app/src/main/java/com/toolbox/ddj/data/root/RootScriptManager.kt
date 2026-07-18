package com.toolbox.ddj.data.root

import android.content.Context
import com.toolbox.ddj.util.RootUtils
import com.toolbox.ddj.util.ShellResult
import java.io.File

/**
 * Root 脚本部署与执行管理。
 *
 * 流程：
 * 1. 从 assets/root_scripts 解压到应用 filesDir
 * 2. 通过 su 同步到 /data/adb/ddj_toolbox/scripts
 * 3. 以 root 调用对应脚本
 *
 * 模块目录与脚本内 MOD_DIR 保持一致，避免依赖原 webui_info 模块。
 */
class RootScriptManager(private val context: Context) {

    companion object {
        const val MODULE_DIR = "/data/adb/ddj_toolbox"
        const val REMOTE_SCRIPTS_DIR = "$MODULE_DIR/scripts"
        const val ASSET_DIR = "root_scripts"
        private const val VERSION_FILE = "VERSION"
    }

    private val localDir: File
        get() = File(context.filesDir, "root_scripts")

    /** 将 assets 同步到本地 filesDir（无需 root）。 */
    fun extractAssetsToLocal(): Result<File> = runCatching {
        val out = localDir
        if (!out.exists() && !out.mkdirs()) {
            error("无法创建本地脚本目录: ${out.absolutePath}")
        }
        val am = context.assets
        val names = am.list(ASSET_DIR)?.toList().orEmpty()
        if (names.isEmpty()) error("assets/$ASSET_DIR 为空")
        names.forEach { name ->
            am.open("$ASSET_DIR/$name").use { input ->
                File(out, name).outputStream().use { output -> input.copyTo(output) }
            }
        }
        out
    }

    /**
     * 确保脚本已部署到设备模块目录。
     * 版本一致且远程存在则跳过拷贝。
     */
    fun ensureDeployed(): DeployResult {
        if (!RootUtils.isRootGranted()) {
            return DeployResult(
                ok = false,
                message = "需要 Root 权限才能部署 ROOT 脚本",
                remoteDir = REMOTE_SCRIPTS_DIR
            )
        }
        val local = extractAssetsToLocal().getOrElse {
            return DeployResult(false, it.message ?: "解压失败", REMOTE_SCRIPTS_DIR)
        }
        val localVersion = File(local, VERSION_FILE).takeIf { it.exists() }?.readText()?.trim().orEmpty()
        val remoteVersionResult = RootUtils.execSu(
            "cat ${RootUtils.shellQuote("$REMOTE_SCRIPTS_DIR/$VERSION_FILE")} 2>/dev/null || true",
            timeoutSec = 5
        )
        val remoteVersion = remoteVersionResult.stdout.trim()
        if (localVersion.isNotEmpty() && localVersion == remoteVersion) {
            // 再确认至少一个脚本在位
            val probe = RootUtils.execSu(
                "[ -f ${RootUtils.shellQuote("$REMOTE_SCRIPTS_DIR/sysinfo.sh")} ] && echo ok",
                timeoutSec = 5
            )
            if (probe.stdout.trim() == "ok") {
                return DeployResult(true, "脚本已是最新（v$localVersion）", REMOTE_SCRIPTS_DIR)
            }
        }

        // 逐文件 cat 复制：弃用 cp（部分设备缺 cp applet，且 src/* 依赖通配符展开两处隐患）。
        // 所有脚本一律经 `sh 脚本` 或 source 调用，无需可执行位，故一并去掉多余的 chmod。
        val files = local.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()
        if (files.isEmpty()) {
            return DeployResult(false, "本地脚本目录为空，无法部署", REMOTE_SCRIPTS_DIR)
        }
        val deployCmd = buildString {
            append("mkdir -p ${RootUtils.shellQuote(REMOTE_SCRIPTS_DIR)}")
            files.forEach { f ->
                append(" && cat ${RootUtils.shellQuote(f.absolutePath)}")
                append(" > ${RootUtils.shellQuote("$REMOTE_SCRIPTS_DIR/${f.name}")}")
            }
            append(" && mkdir -p ${RootUtils.shellQuote(MODULE_DIR)}")
            append(" && touch ${RootUtils.shellQuote("$MODULE_DIR/settings.conf")}")
            append(" && echo deployed")
        }

        val result = RootUtils.execSu(deployCmd, timeoutSec = 30)
        return if (result.stdout.contains("deployed") || result.isSuccess) {
            DeployResult(true, "已部署 ROOT 脚本（v${localVersion.ifEmpty { "?" }}）", REMOTE_SCRIPTS_DIR)
        } else {
            DeployResult(
                ok = false,
                message = "部署失败: ${result.displayText()}",
                remoteDir = REMOTE_SCRIPTS_DIR
            )
        }
    }

    /**
     * 执行已部署的脚本。
     * @param scriptFile 文件名，如 `serial.sh`
     */
    fun runScript(
        scriptFile: String,
        vararg args: String,
        timeoutSec: Long = 45,
        autoDeploy: Boolean = true
    ): ShellResult {
        if (autoDeploy) {
            val deploy = ensureDeployed()
            if (!deploy.ok) {
                return ShellResult(-1, "", deploy.message)
            }
        }
        val path = "$REMOTE_SCRIPTS_DIR/$scriptFile"
        return RootUtils.execScript(path, *args, timeoutSec = timeoutSec)
    }

    data class DeployResult(
        val ok: Boolean,
        val message: String,
        val remoteDir: String
    )
}
