#!/system/bin/sh
# 防设备标记监控脚本（安全版）
# 作者: 呆呆 | QQ:891354018 | 群:774886621
#
# 安全改进：
#   - 不再 bind 整个 /mnt/vendor/persist（避免遮盖密钥/指纹/锁屏数据）
#   - 只 bind /mnt/vendor/persist/data 下的游戏特征子目录
#   - 内置保护名单：keystore / fingerprint / DRM / TEE 等永不 bind

MOD_DIR="/data/adb/ddj_toolbox"
CONFIG_JSON="/data/adb/webui_antimark_config.json"
PERSIST_CONFIG="/data/adb/webui_persist_config.json"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "antimark"
PID_FILE="/data/local/tmp/webui_antimark.pid"
STATUS_FILE="/data/local/tmp/webui_antimark_status"
ENABLE_FILE="/data/local/tmp/webui_antimark_enable"
EMPTY_DIR_BASE="/data/local/tmp/webui_antimark_empty"
MOUNT_LIST_FILE="/data/local/tmp/webui_antimark_mounts"

TARGET_DIR="/mnt/vendor/persist/data"

ACTION="$1"

GAME_LIST_BUILTIN="com.tencent.tmgp.sgame
com.tencent.tmgp.codev
com.tencent.tmgp.cf
com.tencent.tmgp.dfm
com.tencent.tmgp.pubgmhd
com.tencent.mf.uam
com.proxima.dfm
com.garena.game.df
com.tencent.jkchess
com.tencent.tmgp.cod
com.tencent.ngr
com.proximabeta.mf.uamo
com.tw.mf.uamo.kbinstaller
com.vng.mf.uamo"

# ============ 工具函数 ============
log() {
    log_json "$1"
}

read_enable() {
    cat "$ENABLE_FILE" 2>/dev/null | tr -d ' \n\r'
}

write_enable() {
    echo "$1" > "$ENABLE_FILE"
}

is_daemon_alive() {
    [ -f "$PID_FILE" ] || return 1
    local pid
    pid=$(cat "$PID_FILE" 2>/dev/null | tr -d ' \n\r')
    [ -z "$pid" ] && return 1
    if kill -0 "$pid" 2>/dev/null; then
        echo "$pid"
        return 0
    fi
    return 1
}

get_custom_packages() {
    [ -f "$PERSIST_CONFIG" ] || return
    grep -oE '"custom_packages" *: *\[[^]]*\]' "$PERSIST_CONFIG" 2>/dev/null | \
        grep -oE '"[a-zA-Z0-9._]+"' | sed 's/"//g'
}

get_all_game_list() {
    printf '%s\n' "$GAME_LIST_BUILTIN"
    get_custom_packages
}

# ============ JSON 配置 ============
init_config() {
    if [ ! -f "$CONFIG_JSON" ]; then
        echo '{"enabled":0,"force_hide":0,"notify_always":0,"auto_clean_ano":0}' > "$CONFIG_JSON"
    fi
}

read_cfg() {
    init_config
    grep -oE "\"$1\" *: *[^,}]+" "$CONFIG_JSON" 2>/dev/null | head -1 | sed 's/.*: *//;s/"//g'
}

write_cfg() {
    local key="$1" val="$2"
    init_config
    local enabled=$(read_cfg enabled);             [ -z "$enabled" ]        && enabled=0
    local force_hide=$(read_cfg force_hide);       [ -z "$force_hide" ]     && force_hide=0
    local notify_always=$(read_cfg notify_always); [ -z "$notify_always" ]  && notify_always=0
    local auto_clean_ano=$(read_cfg auto_clean_ano); [ -z "$auto_clean_ano" ] && auto_clean_ano=0
    case "$key" in
        enabled)        enabled="$val" ;;
        force_hide)     force_hide="$val" ;;
        notify_always)  notify_always="$val" ;;
        auto_clean_ano) auto_clean_ano="$val" ;;
    esac
    printf '{"enabled":%s,"force_hide":%s,"notify_always":%s,"auto_clean_ano":%s}\n' \
        "$enabled" "$force_hide" "$notify_always" "$auto_clean_ano" > "$CONFIG_JSON"
}

# ===== 安全检查：判断目录名是否在保护名单内 =====
# 返回 0 = 受保护（不能 bind），返回 1 = 可以 bind
is_protected() {
    local name="$1"
    case "$name" in
        Dd*|Hd*)
            return 0 ;;
        data_keystore*|keystore*|keymaster*|keymint*)
            return 0 ;;  # 密钥库
        fingerprint*|biometric*|face*|gatekeeper*|weaver*)
            return 0 ;;  # 生物识别 / 锁屏
        cred*|credential*|owner_info*|key_attestation*)
            return 0 ;;  # 系统凭证
        playready*|widevine*|drm*|wv*|pr*|hdcp*)
            return 0 ;;  # DRM 密钥
        factory*|sec_storage*|secro*|trim_area*|tee*)
            return 0 ;;  # 出厂校准 / TEE
        time*|timekeep*|rfs*|modem*|nv*)
            return 0 ;;  # 基带 / 校准
        audio*|sensor*|camera*|display*|battery*)
            return 0 ;;  # 硬件校准
    esac
    return 1
}

# ===== 挂载隐藏（只 bind 游戏特征子目录，不 bind 整个 persist）=====
do_hide() {
    log "do_hide: 开始（安全模式：只 bind 游戏特征目录）"
    [ ! -d "$TARGET_DIR" ] && { log "do_hide: $TARGET_DIR 不存在"; return 1; }

    rm -rf "$EMPTY_DIR_BASE" 2>/dev/null
    mkdir -p "$EMPTY_DIR_BASE"
    > "$MOUNT_LIST_FILE"

    # 拷贝目标目录的属主和 SELinux context
    local td_uid td_gid td_ctx
    td_uid=$(stat -c "%u" "$TARGET_DIR" 2>/dev/null)
    td_gid=$(stat -c "%g" "$TARGET_DIR" 2>/dev/null)
    td_ctx=$(stat -c "%C" "$TARGET_DIR" 2>/dev/null)
    [ -n "$td_uid" ] && [ -n "$td_gid" ] && chown "${td_uid}:${td_gid}" "$EMPTY_DIR_BASE" 2>/dev/null
    [ -n "$td_ctx" ] && chcon "$td_ctx" "$EMPTY_DIR_BASE" 2>/dev/null

    local count=0 protected=0
    for orig in "$TARGET_DIR"/*; do
        [ -e "$orig" ] || continue
        local name="${orig##*/}"

        # 安全检查
        if is_protected "$name"; then
            protected=$((protected + 1))
            log "do_hide: 保护跳过 $name"
            continue
        fi

        # 创建对应的空目录/文件
        local empty="$EMPTY_DIR_BASE/$name"
        if [ -d "$orig" ]; then
            mkdir -p "$empty"
        else
            touch "$empty"
        fi

        # 拷贝原始文件的属主和上下文
        local orig_uid orig_gid orig_ctx
        orig_uid=$(stat -c "%u" "$orig" 2>/dev/null)
        orig_gid=$(stat -c "%g" "$orig" 2>/dev/null)
        orig_ctx=$(stat -c "%C" "$orig" 2>/dev/null)
        [ -n "$orig_uid" ] && [ -n "$orig_gid" ] && chown "${orig_uid}:${orig_gid}" "$empty" 2>/dev/null
        [ -n "$orig_ctx" ] && chcon "$orig_ctx" "$empty" 2>/dev/null

        # bind 挂载
        if mount --bind --make-private "$empty" "$orig" 2>/dev/null; then
            echo "$orig" >> "$MOUNT_LIST_FILE"
            count=$((count + 1))
        fi
    done

    log "do_hide: 完成（隐藏 $count 项，保护跳过 $protected 项）"
}

do_unhide() {
    log "do_unhide: 开始"
    if [ -f "$MOUNT_LIST_FILE" ]; then
        local item
        # 倒序处理（深的先 umount）
        tac "$MOUNT_LIST_FILE" 2>/dev/null > "${MOUNT_LIST_FILE}.rev" || \
            sort -r "$MOUNT_LIST_FILE" > "${MOUNT_LIST_FILE}.rev"

        while IFS= read -r item; do
            if [ -n "$item" ]; then
                umount "$item" 2>/dev/null || umount -l "$item" 2>/dev/null
            fi
        done < "${MOUNT_LIST_FILE}.rev"
        rm -f "$MOUNT_LIST_FILE" "${MOUNT_LIST_FILE}.rev"
    fi
    rm -rf "$EMPTY_DIR_BASE" 2>/dev/null
    log "do_unhide: 完成"
}

# ============ 通知 ============
notify_status() {
    local status="$1" pkg="$2"
    local title="防设备标记" text=""
    if [ "$status" = "ON" ]; then text="已开启 - $pkg"; else text="未运行"; fi
    su 2000 -c "cmd notification post -S bigtext -t '$title' webui_antimark_tag '$text' --flag ongoing" 2>/dev/null
}

clear_notify() {
    su 2000 -c "cmd notification cancel webui_antimark_tag webui_antimark_tag" 2>/dev/null
    su 2000 -c "cmd notification cancel webui_antimark_tag" 2>/dev/null
}

# ============ ano 清理 ============
clean_ano() {
    local pkg="$1"
    rm -rf "/data/data/${pkg}/files/ano_tmp" 2>/dev/null
    rm -rf /data/user/*/"${pkg}/files/ano_tmp" 2>/dev/null
    log "已清理 ano 文件: $pkg"
}

write_status() {
    if [ "$1" = "1" ]; then
        echo "ON|${2:-unknown}" > "$STATUS_FILE"
    else
        echo "OFF" > "$STATUS_FILE"
    fi
}

# ============ 守护主循环 ============
run_daemon() {
    echo "$$" > "$PID_FILE"
    log "===== 守护启动 PID=$$ ====="

    local STATE=0 LAST_PKG=""

    while true; do
        local enabled_now
        enabled_now=$(read_enable)

        if [ "$enabled_now" != "1" ]; then
            log "检测到 enable=0，守护退出"
            do_unhide
            clear_notify
            write_status 0
            rm -f "$PID_FILE"
            exit 0
        fi

        echo "$$" > "$PID_FILE"

        local force_hide=$(read_cfg force_hide)
        local notify_always=$(read_cfg notify_always)
        local auto_clean_ano=$(read_cfg auto_clean_ano)

        # 强制隐藏模式
        if [ "$force_hide" = "1" ]; then
            if [ "$STATE" -eq 0 ]; then
                log "强制隐藏已启用"
                do_hide
                STATE=1
                write_status 1 "force_mode"
                notify_status "ON" "强制模式"
            fi
            sleep 3
            continue
        else
            if [ "$STATE" -eq 1 ]; then
                local found=0
                for pkg in $(get_all_game_list); do
                    if pidof "$pkg" >/dev/null 2>&1; then found=1; break; fi
                done
                if [ "$found" -eq 0 ]; then
                    log "强制隐藏关闭，无游戏，恢复"
                    do_unhide
                    STATE=0; write_status 0
                    if [ -n "$LAST_PKG" ] && [ "$auto_clean_ano" = "1" ]; then clean_ano "$LAST_PKG"; fi
                    LAST_PKG=""
                    if [ "$notify_always" = "1" ]; then notify_status "OFF" ""; else clear_notify; fi
                fi
            fi
        fi

        # 游戏进程检测
        if [ "$force_hide" != "1" ]; then
            local found=0 cur_pkg=""
            for pkg in $(get_all_game_list); do
                if pidof "$pkg" >/dev/null 2>&1; then
                    found=1; cur_pkg="$pkg"; break
                fi
            done

            if [ "$found" -eq 1 ] && [ "$STATE" -eq 0 ]; then
                log "检测到游戏: $cur_pkg，开启隐藏"
                do_hide; notify_status "ON" "$cur_pkg"
                STATE=1; LAST_PKG="$cur_pkg"; write_status 1 "$cur_pkg"
            elif [ "$found" -eq 0 ] && [ "$STATE" -eq 1 ]; then
                log "游戏退出，关闭隐藏"
                do_unhide
                STATE=0; write_status 0
                if [ -n "$LAST_PKG" ] && [ "$auto_clean_ano" = "1" ]; then clean_ano "$LAST_PKG"; fi
                LAST_PKG=""
                if [ "$notify_always" = "1" ]; then notify_status "OFF" ""; else clear_notify; fi
            fi

            if [ "$notify_always" = "1" ] && [ "$STATE" -eq 0 ]; then notify_status "OFF" ""; fi
        fi

        sleep 3
    done
}

# ============ 启动守护 ============
launch_daemon() {
    rm -f "$PID_FILE"
    write_enable 1

    if command -v setsid >/dev/null 2>&1; then
        setsid nohup sh "$0" _daemon </dev/null >/dev/null 2>&1 &
    elif command -v nohup >/dev/null 2>&1; then
        nohup sh "$0" _daemon </dev/null >/dev/null 2>&1 &
    else
        sh "$0" _daemon </dev/null >/dev/null 2>&1 &
    fi

    local i=0
    while [ $i -lt 30 ]; do
        if [ -f "$PID_FILE" ]; then
            local pid
            pid=$(cat "$PID_FILE" 2>/dev/null | tr -d ' \n\r')
            if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
                echo "$pid"
                return 0
            fi
        fi
        sleep 0.1 2>/dev/null || sleep 1
        i=$((i + 1))
    done
    return 1
}

# ============ 命令入口 ============
cmd_start() {
    init_config

    local live_pid
    if live_pid=$(is_daemon_alive); then
        write_enable 1
        write_cfg enabled 1
        printf '{"ok":true,"msg":"已在运行","pid":"%s"}\n' "$live_pid"
        return
    fi

    write_cfg enabled 1
    local pid
    if pid=$(launch_daemon); then
        log "守护启动成功 PID=$pid"
        printf '{"ok":true,"msg":"防标记已启动","pid":"%s"}\n' "$pid"
    else
        log "守护启动失败"
        write_enable 0
        write_cfg enabled 0
        echo '{"ok":false,"error":"守护启动失败，请查看日志"}'
    fi
}

cmd_stop() {
    write_enable 0
    write_cfg enabled 0

    if [ -f "$PID_FILE" ]; then
        local old_pid
        old_pid=$(cat "$PID_FILE" 2>/dev/null | tr -d ' \n\r')
        if [ -n "$old_pid" ] && kill -0 "$old_pid" 2>/dev/null; then
            kill "$old_pid" 2>/dev/null
            sleep 1
            kill -0 "$old_pid" 2>/dev/null && kill -9 "$old_pid" 2>/dev/null
        fi
        rm -f "$PID_FILE"
    fi

    local stray
    stray=$(pgrep -f "anti-mark.sh _daemon" 2>/dev/null)
    if [ -n "$stray" ]; then
        echo "$stray" | while read -r p; do
            [ "$p" != "$old_pid" ] && kill -9 "$p" 2>/dev/null
        done
    fi

    do_unhide
    clear_notify
    write_status 0
    log "守护已停止"
    echo '{"ok":true,"msg":"防标记已停止"}'
}

cmd_status() {
    init_config
    local force_hide=$(read_cfg force_hide);       [ -z "$force_hide" ]     && force_hide=0
    local notify_always=$(read_cfg notify_always); [ -z "$notify_always" ]  && notify_always=0
    local auto_clean_ano=$(read_cfg auto_clean_ano); [ -z "$auto_clean_ano" ] && auto_clean_ano=0

    local enabled
    enabled=$(read_enable)
    [ "$enabled" != "1" ] && enabled=0

    local running=false pid=""
    if pid=$(is_daemon_alive); then
        running=true
    fi

    if [ "$enabled" = "1" ] && [ "$running" = "false" ]; then
        log "状态自愈：enable=1 但进程不存在，自动重启守护"
        local new_pid
        if new_pid=$(launch_daemon); then
            pid="$new_pid"
            running=true
            log "守护已恢复 PID=$new_pid"
        else
            write_enable 0
            write_cfg enabled 0
            enabled=0
        fi
    fi

    local mark_state="OFF" mark_pkg=""
    if [ -f "$STATUS_FILE" ]; then
        local raw
        raw=$(cat "$STATUS_FILE" 2>/dev/null)
        mark_state=$(echo "$raw" | cut -d'|' -f1)
        mark_pkg=$(echo "$raw" | cut -d'|' -f2 2>/dev/null)
    fi

    printf '{"ok":true,"enabled":%s,"running":%s,"pid":"%s","force_hide":%s,"notify_always":%s,"auto_clean_ano":%s,"state":"%s","game":"%s"}\n' \
        "$enabled" "$running" "$pid" "$force_hide" "$notify_always" "$auto_clean_ano" "$mark_state" "$mark_pkg"
}

cmd_boot_apply() {
    local enabled
    enabled=$(read_enable)
    [ "$enabled" != "1" ] && return

    if is_daemon_alive >/dev/null; then
        log "boot_apply: 守护已在运行，跳过"
        return
    fi

    log "boot_apply: enable=1，自动恢复守护"
    launch_daemon >/dev/null
}

cmd_set() {
    local key="$2" val="$3"
    if [ -z "$key" ] || [ -z "$val" ]; then
        echo '{"ok":false,"error":"missing key or value"}'
        return 1
    fi
    case "$key" in
        enabled|force_hide|notify_always|auto_clean_ano)
            write_cfg "$key" "$val"
            printf '{"ok":true,"key":"%s","val":%s}\n' "$key" "$val"
            ;;
        *)
            echo '{"ok":false,"error":"unknown key"}'
            return 1 ;;
    esac
}

cmd_get_config() {
    init_config
    cat "$CONFIG_JSON"
}

cmd_log() {
    read_log_text 30
}

case "$ACTION" in
    start)      cmd_start ;;
    stop)       cmd_stop ;;
    status)     cmd_status ;;
    boot_apply) cmd_boot_apply ;;
    set)        cmd_set "$@" ;;
    get_config) cmd_get_config ;;
    log)        cmd_log ;;
    _daemon)    run_daemon ;;
    *)
        echo '{"ok":false,"error":"unknown action - use: start|stop|status|boot_apply|set|get_config|log"}'
        exit 1 ;;
esac
