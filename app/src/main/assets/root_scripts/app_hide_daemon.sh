#!/system/bin/sh
# 应用控制守护进程 - 监控触发应用启动/退出并执行冻结+隐藏
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 命令：pm disable-user --user 0 <pkg> + pm hide <pkg>

MOD_DIR="/data/adb/ddj_toolbox"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "app_hide"

RULES_FILE="/data/adb/webui_app_rules.json"
PID_FILE="/data/local/tmp/webui_app_hide_daemon.pid"
APPLIED_FILE="/data/local/tmp/webui_app_hide_applied.list"
POLL_INTERVAL=2

# 记录当前已触发的trigger（避免重复执行）
TRIGGERED=""
SHUTTING_DOWN=0

write_pid() {
    echo "$$" > "$PID_FILE"
}

# 获取当前前台应用包名
get_foreground_pkg() {
    local pkg
    pkg=$(dumpsys activity activities 2>/dev/null | grep -m1 -E "topResumedActivity|mResumedActivity" | grep -oE '[a-zA-Z][a-zA-Z0-9._]+/' | head -1 | tr -d '/')
    if [ -z "$pkg" ]; then
        pkg=$(dumpsys window 2>/dev/null | grep -m1 -E "mCurrentFocus|mFocusedApp" | grep -oE '[a-zA-Z][a-zA-Z0-9._]+/' | head -1 | tr -d '/')
    fi
    echo "$pkg"
}

is_foreground() {
    [ "$1" = "$2" ] && [ -n "$1" ]
}

# 冻结+隐藏目标应用
freeze_hide() {
    local pkg="$1"
    pm disable-user --user 0 "$pkg" >/dev/null 2>&1
    pm hide "$pkg" >/dev/null 2>&1
    log_json "冻结+隐藏: $pkg"
}

# 解冻+恢复目标应用
unfreeze_unhide() {
    local pkg="$1"
    pm enable --user 0 "$pkg" >/dev/null 2>&1
    pm unhide "$pkg" >/dev/null 2>&1
    log_json "解冻+恢复: $pkg"
}

# 检查trigger是否在已触发列表中
is_triggered() {
    echo "$TRIGGERED" | grep -q "|$1|"
}

mark_triggered() {
    if ! is_triggered "$1"; then
        TRIGGERED="${TRIGGERED}|$1|"
    fi
}

unmark_triggered() {
    TRIGGERED=$(echo "$TRIGGERED" | sed "s/|$1|//g")
}

record_applied() {
    local trigger="$1"
    local target="$2"
    [ -z "$trigger" ] || [ -z "$target" ] && return
    if [ ! -f "$APPLIED_FILE" ] || ! grep -qx "$trigger $target" "$APPLIED_FILE" 2>/dev/null; then
        printf '%s %s\n' "$trigger" "$target" >> "$APPLIED_FILE"
    fi
}

remove_applied() {
    local trigger="$1"
    local target="$2"
    local tmp="${APPLIED_FILE}.$$"
    [ -f "$APPLIED_FILE" ] || return
    grep -vx "$trigger $target" "$APPLIED_FILE" 2>/dev/null > "$tmp"
    mv "$tmp" "$APPLIED_FILE"
    [ -s "$APPLIED_FILE" ] || rm -f "$APPLIED_FILE"
}

get_applied_targets_for_trigger() {
    local trigger="$1"
    [ -f "$APPLIED_FILE" ] || return
    while read -r saved_trigger saved_target; do
        [ "$saved_trigger" = "$trigger" ] && [ -n "$saved_target" ] && echo "$saved_target"
    done < "$APPLIED_FILE"
}

# 获取某trigger的所有目标
get_targets_for_trigger() {
    local trigger="$1"
    grep -oE "\{\"trigger\":\"$trigger\",\"target\":\"[^\"]*\"\}" "$RULES_FILE" 2>/dev/null | \
        grep -oE '"target":"[^"]*"' | sed 's/"target":"//;s/"//'
}

restore_trigger_targets() {
    local trigger="$1"
    local targets
    targets=$(get_applied_targets_for_trigger "$trigger")
    if [ -z "$targets" ]; then
        targets=$(get_targets_for_trigger "$trigger")
    fi
    for target in $targets; do
        [ -z "$target" ] && continue
        unfreeze_unhide "$target"
        remove_applied "$trigger" "$target"
    done
}

restore_all_applied() {
    [ -f "$APPLIED_FILE" ] || return
    local tmp="${APPLIED_FILE}.$$"
    cp "$APPLIED_FILE" "$tmp" 2>/dev/null || return
    while read -r saved_trigger saved_target; do
        [ -z "$saved_target" ] && continue
        unfreeze_unhide "$saved_target"
        remove_applied "$saved_trigger" "$saved_target"
    done < "$tmp"
    rm -f "$tmp" "$APPLIED_FILE"
}

sync_stale_applied() {
    [ -f "$APPLIED_FILE" ] || return
    local tmp="${APPLIED_FILE}.$$"
    cp "$APPLIED_FILE" "$tmp" 2>/dev/null || return
    local fg
    fg=$(get_foreground_pkg)
    while read -r saved_trigger saved_target; do
        [ -z "$saved_trigger" ] || [ -z "$saved_target" ] && continue
        if [ "$saved_trigger" = "$fg" ]; then
            mark_triggered "$saved_trigger"
            log_json "重启后继续监控: $saved_trigger -> $saved_target"
        else
            unfreeze_unhide "$saved_target"
            remove_applied "$saved_trigger" "$saved_target"
            log_json "重启后兜底恢复: $saved_trigger -> $saved_target"
        fi
    done < "$tmp"
    rm -f "$tmp"
}

shutdown_daemon() {
    [ "$SHUTTING_DOWN" = "1" ] && exit 0
    SHUTTING_DOWN=1
    log_json "收到停止信号，正在兜底恢复"
    restore_all_applied
    rm -f "$PID_FILE"
    exit 0
}

trap '' HUP
trap 'shutdown_daemon' INT TERM EXIT

write_pid
sync_stale_applied
log_json "守护进程启动 PID=$$"

while true; do
    if [ ! -f "$RULES_FILE" ]; then
        sleep "$POLL_INTERVAL"
        continue
    fi

    fg_pkg=$(get_foreground_pkg)
    triggers=$(grep -oE '"trigger":"[^"]*"' "$RULES_FILE" 2>/dev/null | sed 's/"trigger":"//;s/"//' | sort -u)

    for trigger in $triggers; do
        if [ "$fg_pkg" = "$trigger" ]; then
            if ! is_triggered "$trigger"; then
                mark_triggered "$trigger"
                log_json "检测到触发应用前台: $trigger"
                targets=$(get_targets_for_trigger "$trigger")
                for target in $targets; do
                    [ -z "$target" ] && continue
                    freeze_hide "$target"
                    record_applied "$trigger" "$target"
                done
            fi
        else
            if is_triggered "$trigger"; then
                unmark_triggered "$trigger"
                log_json "触发应用离开前台: $trigger"
                restore_trigger_targets "$trigger"
            fi
        fi
    done

    sleep "$POLL_INTERVAL"
done
