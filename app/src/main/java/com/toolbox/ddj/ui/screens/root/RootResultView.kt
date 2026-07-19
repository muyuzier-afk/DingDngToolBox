package com.toolbox.ddj.ui.screens.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.ddj.R
import com.toolbox.ddj.data.root.FieldKind
import com.toolbox.ddj.data.root.ResultField
import com.toolbox.ddj.data.root.ResultTable
import com.toolbox.ddj.data.root.RootResult
import com.toolbox.ddj.ui.components.InfoRow
import com.toolbox.ddj.ui.components.InfoSectionCard
import com.toolbox.ddj.ui.components.StatusBanner
import com.toolbox.ddj.ui.components.TonalCard
import org.json.JSONObject

/**
 * ROOT 脚本结果的数据驱动语义渲染器。
 *
 * 把 [RootResult] 按「状态横幅 + 重启警告 + 键值卡 + 数组表 + 原始 JSON」渲染，
 * 复用 [InfoSectionCard] / [InfoRow] / [StatusBanner]。解析失败自动回退等宽文本。
 *
 * 精修（数据驱动，不按 toolId 硬编码）：
 * - 数组元素含 pkg + label → 应用卡样式；
 * - toolId == "sysinfo" → 设备信息按 硬件/系统/安全 分组（唯一的轻特化，因其字段最杂）。
 */
@Composable
fun RootResultView(
    result: RootResult?,
    toolId: String,
    modifier: Modifier = Modifier
) {
    if (result == null) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1) 状态横幅：仅当有 ok 字段
        result.ok?.let { ok ->
            StatusBanner(
                message = result.message ?: stringResource(
                    if (ok) R.string.root_result_success else R.string.root_result_failed
                ),
                containerColor = if (ok) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (ok) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onErrorContainer
            )
        }
        // 无 ok 但有 message：中性提示
        if (result.ok == null && !result.message.isNullOrBlank()) {
            StatusBanner(
                message = result.message,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        // 2) 重启警告（M3 无标准 warning 槽，用固定警示橙保证跨动态主题一致）
        if (result.reboot) {
            StatusBanner(
                message = stringResource(R.string.root_result_reboot),
                containerColor = Color(0xFFFFB74D),
                contentColor = Color(0xFF3E2200)
            )
        }
        // 3) 键值字段
        if (result.fields.isNotEmpty()) {
            if (toolId == "sysinfo") {
                SysinfoGroups(result.fields)
            } else {
                InfoSectionCard(title = stringResource(R.string.root_result_detail)) {
                    result.fields.forEach { FieldRow(it) }
                }
            }
        }
        // 4) 数组表
        result.tables.forEach { table -> ResultTableCard(table) }

        // 5) 原始输出：解析成功折叠、纯文本回退直接展示
        if (result.parsedAsJson) {
            RawJsonSection(result.raw)
        } else if (result.raw.isNotBlank()) {
            InfoSectionCard(title = stringResource(R.string.root_output)) {
                MonospaceText(result.raw)
            }
        }
    }
}

/** 键值卡里的单字段行：BOOL 用徽章，其余用 InfoRow（label 上、value 次级色下）。 */
@Composable
private fun FieldRow(field: ResultField) {
    if (field.kind == FieldKind.BOOL) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(field.label, style = MaterialTheme.typography.bodyLarge)
            BoolBadge(field.value == "true")
        }
    } else {
        InfoRow(field.label, field.value)
    }
}

/** 布尔徽章：真=绿"是"，假=灰"否"。 */
@Composable
private fun BoolBadge(on: Boolean) {
    val container = if (on) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (on) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(if (on) R.string.root_bool_yes else R.string.root_bool_no),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 数组字段 → 一张卡；元素像应用（含 pkg+label）走应用卡样式，否则通用行。 */
@Composable
private fun ResultTableCard(table: ResultTable) {
    InfoSectionCard(title = "${table.label}（${table.rows.size}）") {
        table.rows.forEachIndexed { idx, row ->
            if (idx > 0) ThinDivider()
            if (row.isAppRow()) AppRow(row) else GenericRow(row)
        }
    }
}

private fun List<ResultField>.isAppRow(): Boolean =
    any { it.key == "pkg" } && any { it.key == "label" }

/** 应用卡：图标占位圆 + 应用名 + 包名 + 其余字段。 */
@Composable
private fun AppRow(row: List<ResultField>) {
    val name = row.firstOrNull { it.key == "label" }?.value?.takeIf { it.isNotBlank() } ?: "?"
    val pkg = row.firstOrNull { it.key == "pkg" }?.value.orEmpty()
    val extras = row.filter { it.key != "label" && it.key != "pkg" && it.key != "icon" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (pkg.isNotBlank()) {
                Text(
                    text = pkg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            extras.forEach { e ->
                Text(
                    text = "${e.label}：${e.value}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 通用表行：每字段 label 左 / value 右，BOOL 用徽章。 */
@Composable
private fun GenericRow(row: List<ResultField>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        row.forEach { f ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = f.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                if (f.kind == FieldKind.BOOL) {
                    Spacer(Modifier.weight(1f))
                    BoolBadge(f.value == "true")
                } else {
                    Text(
                        text = f.value,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** 设备信息（sysinfo）分组：硬件 / 系统 / 安全 / 其他。 */
@Composable
private fun SysinfoGroups(fields: List<ResultField>) {
    val byKey = fields.associateBy { it.key }
    val used = mutableSetOf<String>()
    SYSINFO_GROUPS.forEach { (titleRes, keys) ->
        val groupFields = keys.mapNotNull { byKey[it] }
        if (groupFields.isNotEmpty()) {
            used.addAll(groupFields.map { it.key })
            InfoSectionCard(title = stringResource(titleRes)) {
                groupFields.forEach { FieldRow(it) }
            }
        }
    }
    val rest = fields.filter { it.key !in used }
    if (rest.isNotEmpty()) {
        InfoSectionCard(title = stringResource(R.string.root_result_other)) {
            rest.forEach { FieldRow(it) }
        }
    }
}

private val SYSINFO_GROUPS: List<Pair<Int, List<String>>> = listOf(
    R.string.root_group_hardware to listOf(
        "brand", "model", "manufacturer", "device", "board",
        "hardware", "cpu_abi", "screen", "density", "ram_mb", "battery"
    ),
    R.string.root_group_system to listOf(
        "android", "sdk", "kernel", "build_id", "build_date",
        "fingerprint", "bootloader", "timezone", "lang", "uptime"
    ),
    R.string.root_group_security to listOf("security_patch", "selinux", "magisk")
)

/** 原始 JSON 折叠区。 */
@Composable
private fun RawJsonSection(raw: String) {
    var expanded by remember { mutableStateOf(false) }
    TonalCard {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.root_result_raw),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                MonospaceText(prettyRaw(raw))
            }
        }
    }
}

@Composable
private fun MonospaceText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    )
}

@Composable
private fun ThinDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

private fun prettyRaw(raw: String): String = try {
    val t = raw.trim()
    if (t.startsWith("{")) JSONObject(t).toString(2) else raw
} catch (_: Exception) {
    raw
}
