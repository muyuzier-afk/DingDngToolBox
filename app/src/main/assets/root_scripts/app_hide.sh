#!/system/bin/sh
# 应用控制脚本 - 基于触发器的隐藏/冻结
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 逻辑：当触发应用启动时，自动隐藏/冻结指定的目标应用；触发应用退出时恢复
# 用法:
#   sh app_hide.sh list_rules            列出所有规则
#   sh app_hide.sh add_rule <trigger> <target> <action>  添加规则(action=hide|freeze)
#   sh app_hide.sh del_rule <trigger> <target>           删除规则
#   sh app_hide.sh del_trigger <trigger>                 删除触发器所有规则
#   sh app_hide.sh list_apps                             列出第三方应用
#   sh app_hide.sh daemon_start                          启动守护进程
#   sh app_hide.sh daemon_stop                           停止守护进程
#   sh app_hide.sh daemon_status                         守护进程状态

MOD_DIR="/data/adb/ddj_toolbox"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "app_hide"

RULES_FILE="/data/adb/webui_app_rules.json"
PID_FILE="/data/local/tmp/webui_app_hide_daemon.pid"
APPLIED_FILE="/data/local/tmp/webui_app_hide_applied.list"
DAEMON_LOG="/data/local/tmp/webui_app_hide_daemon.log"
LAUNCHER_FILE="/data/local/tmp/webui_app_hide_launcher.sh"
SETTINGS_FILE="$MOD_DIR/settings.conf"
ENABLED_KEY="app_hide_daemon_enabled"

write_enabled() {
    local val="$1"
    [ ! -f "$SETTINGS_FILE" ] && touch "$SETTINGS_FILE" 2>/dev/null
    if grep -q "^${ENABLED_KEY}=" "$SETTINGS_FILE" 2>/dev/null; then
        sed -i "s|^${ENABLED_KEY}=.*|${ENABLED_KEY}=${val}|" "$SETTINGS_FILE"
    else
        echo "${ENABLED_KEY}=${val}" >> "$SETTINGS_FILE"
    fi
}

ACTION="$1"
ARG2="$2"
ARG3="$3"
ARG4="$4"

# 初始化规则文件
init_rules() {
    if [ ! -f "$RULES_FILE" ]; then
        echo '{"rules":[]}' > "$RULES_FILE"
    fi
}

is_pid_alive() {
    local pid="$1"
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

restore_applied_targets() {
    [ -f "$APPLIED_FILE" ] || return
    local tmp="${APPLIED_FILE}.$$"
    cp "$APPLIED_FILE" "$tmp" 2>/dev/null || return
    while read -r trigger target; do
        [ -z "$target" ] && continue
        pm enable --user 0 "$target" >/dev/null 2>&1
        pm unhide "$target" >/dev/null 2>&1
        log_json "兜底恢复: $trigger -> $target"
    done < "$tmp"
    rm -f "$tmp" "$APPLIED_FILE"
}

wait_daemon_pid() {
    local i=0
    local pid=""
    while [ "$i" -lt 5 ]; do
        if [ -f "$PID_FILE" ]; then
            pid=$(cat "$PID_FILE" 2>/dev/null)
            if is_pid_alive "$pid"; then
                echo "$pid"
                return 0
            fi
        fi
        i=$((i + 1))
        sleep 1
    done
    return 1
}

# 列出所有规则
cmd_list_rules() {
    init_rules
    cat "$RULES_FILE"
}

# 添加规则: add_rule <trigger_pkg> <target_pkg>
cmd_add_rule() {
    local trigger="$ARG2"
    local target="$ARG3"
    if [ -z "$trigger" ] || [ -z "$target" ]; then
        echo '{"ok":false,"error":"缺少触发应用或目标应用包名"}'
        return
    fi

    init_rules

    # 检查是否已存在相同规则
    local exists
    exists=$(grep -oE "\{\"trigger\":\"$trigger\",\"target\":\"$target\"\}" "$RULES_FILE")
    if [ -n "$exists" ]; then
        echo '{"ok":false,"error":"规则已存在"}'
        return
    fi

    # 读取现有规则数组
    local old_rules
    old_rules=$(grep -oE '\{\"trigger\":\"[^"]*\",\"target\":\"[^"]*\"\}' "$RULES_FILE")

    local new_entry="{\"trigger\":\"${trigger}\",\"target\":\"${target}\"}"
    local all=""
    local sep=""

    if [ -n "$old_rules" ]; then
        all=$(echo "$old_rules" | tr '\n' ',' | sed 's/,$//')
        sep=","
    fi
    all="${all}${sep}${new_entry}"

    printf '{"rules":[%s]}\n' "$all" > "$RULES_FILE"
    log_json "添加规则: $trigger 启动时 冻结+隐藏 $target"
    printf '{"ok":true,"msg":"规则已添加"}\n'
}

# 删除规则: del_rule <trigger> <target>
cmd_del_rule() {
    local trigger="$ARG2"
    local target="$ARG3"
    if [ -z "$trigger" ] || [ -z "$target" ]; then
        echo '{"ok":false,"error":"缺少触发应用或目标应用包名"}'
        return
    fi
    init_rules

    local remaining
    remaining=$(grep -oE '\{\"trigger\":\"[^"]*\",\"target\":\"[^"]*\"\}' "$RULES_FILE" | \
        grep -v "\"trigger\":\"$trigger\",\"target\":\"$target\"")

    if [ -z "$remaining" ]; then
        echo '{"rules":[]}' > "$RULES_FILE"
    else
        local entries
        entries=$(echo "$remaining" | tr '\n' ',' | sed 's/,$//')
        printf '{"rules":[%s]}\n' "$entries" > "$RULES_FILE"
    fi
    log_json "删除规则: $trigger -> $target"
    echo '{"ok":true,"msg":"规则已删除"}'
}

# 删除某触发器的所有规则
cmd_del_trigger() {
    local trigger="$ARG2"
    if [ -z "$trigger" ]; then
        echo '{"ok":false,"error":"缺少触发应用包名"}'
        return
    fi
    init_rules

    local remaining
    remaining=$(grep -oE '\{\"trigger\":\"[^"]*\",\"target\":\"[^"]*\"\}' "$RULES_FILE" | \
        grep -v "\"trigger\":\"$trigger\"")

    if [ -z "$remaining" ]; then
        echo '{"rules":[]}' > "$RULES_FILE"
    else
        local entries
        entries=$(echo "$remaining" | tr '\n' ',' | sed 's/,$//')
        printf '{"rules":[%s]}\n' "$entries" > "$RULES_FILE"
    fi
    log_json "删除触发器所有规则: $trigger"
    echo '{"ok":true,"msg":"已删除该触发器的所有规则"}'
}

# 立即测试：手动对目标执行冻结+隐藏（pm disable-user + pm hide）
cmd_test_apply() {
    local target="$ARG2"
    if [ -z "$target" ]; then
        echo '{"ok":false,"error":"缺少目标包名"}'
        return
    fi
    pm disable-user --user 0 "$target" >/dev/null 2>&1
    pm hide "$target" >/dev/null 2>&1
    log_json "手动冻结+隐藏: $target"
    printf '{"ok":true,"msg":"已冻结+隐藏 %s"}\n' "$target"
}

# 手动恢复
cmd_test_restore() {
    local target="$ARG2"
    if [ -z "$target" ]; then
        echo '{"ok":false,"error":"缺少目标包名"}'
        return
    fi
    pm enable --user 0 "$target" >/dev/null 2>&1
    pm unhide "$target" >/dev/null 2>&1
    log_json "手动解冻+恢复: $target"
    printf '{"ok":true,"msg":"已恢复 %s"}\n' "$target"
}

# 列出第三方应用
cmd_list_apps() {
    local pkgs
    pkgs=$(pm list packages -3 2>/dev/null | sed 's/^package://' | sort)
    local json=""
    local sep=""
    local count=0
    for pkg in $pkgs; do
        json="${json}${sep}\"${pkg}\""
        sep=","
        count=$((count + 1))
    done
    printf '{"ok":true,"count":%d,"packages":[%s]}\n' "$count" "$json"
}

# 全量恢复：解冻规则中所有 target + 清理 applied.list（开机兜底用）
cmd_restore_all() {
    restore_applied_targets
    local count=0
    if [ -f "$RULES_FILE" ]; then
        local targets
        targets=$(grep -oE '"target":"[^"]*"' "$RULES_FILE" 2>/dev/null | sed 's/"target":"//;s/"//' | sort -u)
        for target in $targets; do
            [ -z "$target" ] && continue
            pm enable --user 0 "$target" >/dev/null 2>&1
            pm unhide "$target" >/dev/null 2>&1
            count=$((count + 1))
        done
    fi
    log_json "全量恢复隐藏应用 ($count 个)"
    printf '{"ok":true,"msg":"已全量恢复","count":%d}\n' "$count"
}

# 守护进程 - 启动
cmd_daemon_start() {
    if [ -f "$PID_FILE" ]; then
        local old_pid
        old_pid=$(cat "$PID_FILE" 2>/dev/null)
        if is_pid_alive "$old_pid"; then
            echo '{"ok":false,"error":"守护进程已在运行 (PID:'$old_pid')"}'
            return
        fi
        rm -f "$PID_FILE"
    fi

    cat > "$LAUNCHER_FILE" <<EOF
#!/system/bin/sh
trap '' HUP
export PATH=/system/bin:/system/xbin:/vendor/bin:/vendor/xbin:/product/bin:/apex/com.android.runtime/bin:\$PATH
cd /
sleep 1
exec sh "$MOD_DIR/scripts/app_hide_daemon.sh" </dev/null >>"$DAEMON_LOG" 2>&1
EOF
    chmod 755 "$LAUNCHER_FILE" 2>/dev/null

    if command -v setsid >/dev/null 2>&1; then
        nohup setsid sh "$LAUNCHER_FILE" </dev/null >/dev/null 2>&1 &
    elif command -v nohup >/dev/null 2>&1; then
        nohup sh "$LAUNCHER_FILE" </dev/null >/dev/null 2>&1 &
    else
        ( trap '' HUP; sh "$LAUNCHER_FILE" </dev/null >/dev/null 2>&1 & ) >/dev/null 2>&1
    fi

    local pid
    pid=$(wait_daemon_pid)
    if [ -z "$pid" ]; then
        log_json "守护进程启动失败，请查看 $DAEMON_LOG"
        echo '{"ok":false,"error":"守护进程启动失败"}'
        return
    fi
    write_enabled 1
    log_json "守护进程已启动 PID=$pid"
    printf '{"ok":true,"msg":"守护进程已启动","pid":%d}\n' "$pid"
}

# 守护进程 - 停止
cmd_daemon_stop() {
    write_enabled 0
    if [ ! -f "$PID_FILE" ]; then
        restore_applied_targets
        echo '{"ok":true,"msg":"守护进程未运行，已执行兜底恢复"}'
        return
    fi
    local pid
    pid=$(cat "$PID_FILE" 2>/dev/null)
    if is_pid_alive "$pid"; then
        kill "$pid" 2>/dev/null
        sleep 1
        if is_pid_alive "$pid"; then
            kill -9 "$pid" 2>/dev/null
        fi
        rm -f "$PID_FILE"
        restore_applied_targets
        log_json "守护进程已停止 PID=$pid"
        echo '{"ok":true,"msg":"守护进程已停止"}'
    else
        rm -f "$PID_FILE"
        restore_applied_targets
        echo '{"ok":true,"msg":"守护进程已清理（进程不存在）"}'
    fi
}

# 守护进程 - 状态
cmd_daemon_status() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE" 2>/dev/null)
        if is_pid_alive "$pid"; then
            printf '{"ok":true,"running":true,"pid":%d}\n' "$pid"
        else
            rm -f "$PID_FILE"
            restore_applied_targets
            echo '{"ok":true,"running":false}'
        fi
    else
        restore_applied_targets
        echo '{"ok":true,"running":false}'
    fi
}

case "$ACTION" in
    list_rules)     cmd_list_rules ;;
    add_rule)       cmd_add_rule ;;
    del_rule)       cmd_del_rule ;;
    del_trigger)    cmd_del_trigger ;;
    list_apps)      cmd_list_apps ;;
    restore_all)    cmd_restore_all ;;
    daemon_start)   cmd_daemon_start ;;
    daemon_stop)    cmd_daemon_stop ;;
    daemon_status)  cmd_daemon_status ;;
    test_apply)     cmd_test_apply ;;
    test_restore)   cmd_test_restore ;;
    *)
        echo '{"ok":false,"error":"unknown action"}'
        ;;
esac
