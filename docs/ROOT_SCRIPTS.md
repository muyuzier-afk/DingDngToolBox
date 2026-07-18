# ROOT 脚本集成说明

## 路径

| 位置 | 路径 |
|------|------|
| 应用内 assets | `app/src/main/assets/root_scripts/` |
| 运行时部署 | `/data/adb/ddj_toolbox/scripts/` |
| 配置 | `/data/adb/ddj_toolbox/settings.conf` |
| 日志 | `/data/adb/ddj_logs/` |

脚本内 `MOD_DIR` 已统一为 `/data/adb/ddj_toolbox`，不依赖原 `webui_info` 模块。

## 部署

`RootScriptManager.ensureDeployed()`：

1. 将 assets 复制到 `filesDir/root_scripts`
2. 对比 `VERSION`，不一致则 `su` 同步到 `/data/adb/ddj_toolbox/scripts`
3. `chmod 755` 所有 `.sh`

## 执行

`RootUtils.execSu` / `execScript` → `su -c 'sh <script> <args...>'`

UI 通过 `RootToolCatalog` 映射工具与动作；危险操作需二次确认。

## 脚本清单

- `sysinfo.sh` / `android_id.sh` / `ssaid.sh` / `oaid.sh` / `serial.sh`
- `spoof.sh` / `presets.sh`
- `anti-mark.sh` / `persist-hide.sh` / `app_hide.sh` / `app_hide_daemon.sh`
- `cleaner.sh` / `monitor.sh`
- `touch_rate.sh` / `touch_daemon.sh`
- `settings.sh` / `lib_log.sh` / `scan_apps.sh`
