#!/system/bin/sh
# Persist 分区特征隐藏脚本
# 作者: 呆呆 | QQ:891354018 | 群:774886621

PATH="$PATH:/data/adb/ksu/bin:/data/adb/magisk"

MOD_DIR="/data/adb/ddj_toolbox"
SETTINGS_FILE="$MOD_DIR/settings.conf"
CONFIG_JSON="/data/adb/webui_persist_config.json"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "persist"
STATE_DIR="/data/local/tmp/webui_persist_state"
EMPTY_DIR="$STATE_DIR/empty_root"
MOUNT_LIST="$STATE_DIR/mount.list"
PID_FILE="$STATE_DIR/monitor.pid"
TARGET_DIR="/mnt/vendor/persist/data"
ACTION="$1"

PKG_LIST_BUILTIN="com.tencent.tmgp.sgame
com.tencent.tmgp.codev
com.tencent.tmgp.cf
com.tencent.tmgp.dfm
com.tencent.tmgp.pubgmhd
com.tencent.mf.uam
com.proxima.dfm
com.garena.game.df
com.tencent.jkchess"

# ===== JSON 配置读写 =====
init_config() {
    if [ ! -f "$CONFIG_JSON" ]; then
        echo '{"persist_hide_enable":0,"custom_packages":[]}' > "$CONFIG_JSON"
    fi
}

read_json_val() {
    local key="$1"
    grep -oE "\"${key}\"\s*:\s*[^,}]+" "$CONFIG_JSON" 2>/dev/null | head -1 | sed 's/.*:\s*//;s/"//g'
}

get_custom_packages() {
    init_config
    grep -oE '"custom_packages"\s*:\s*\[[^]]*\]' "$CONFIG_JSON" 2>/dev/null | \
        grep -oE '"[a-zA-Z0-9._]+"' | sed 's/"//g'
}

get_all_packages() {
    printf '%s\n' "$PKG_LIST_BUILTIN"
    get_custom_packages
}

save_config() {
    local enable="$1"
    local pkgs_json="$2"
    printf '{"persist_hide_enable":%s,"custom_packages":[%s]}\n' "$enable" "$pkgs_json" > "$CONFIG_JSON"
}

add_custom_pkg() {
    local pkg="$1"
    init_config
    local existing
    existing=$(get_custom_packages)
    if echo "$existing" | grep -qx "$pkg"; then
        echo '{"ok":false,"error":"包名已存在"}'
        return 1
    fi
    if echo "$PKG_LIST_BUILTIN" | grep -qx "$pkg"; then
        echo '{"ok":false,"error":"该包名已在内置列表中"}'
        return 1
    fi
    local enable
    enable=$(read_json_val persist_hide_enable)
    [ -z "$enable" ] && enable=0
    local new_list old_json_arr
    old_json_arr=$(get_custom_packages | awk '{printf "\"" $0 "\","}' | sed 's/,$//')
    if [ -n "$old_json_arr" ]; then
        new_list="${old_json_arr},\"${pkg}\""
    else
        new_list="\"${pkg}\""
    fi
    save_config "$enable" "$new_list"
    log "自定义包名已添加: $pkg"
    printf '{"ok":true,"msg":"已添加 %s"}\n' "$pkg"
}

remove_custom_pkg() {
    local pkg="$1"
    init_config
    local enable
    enable=$(read_json_val persist_hide_enable)
    [ -z "$enable" ] && enable=0
    local new_list
    new_list=$(get_custom_packages | grep -v "^${pkg}$" | awk '{printf "\"" $0 "\","}' | sed 's/,$//')
    save_config "$enable" "$new_list"
    log "自定义包名已移除: $pkg"
    printf '{"ok":true,"msg":"已移除 %s"}\n' "$pkg"
}

get_config_json() {
    init_config
    cat "$CONFIG_JSON"
}

read_setting() {
    local key="$1"
    if [ "$key" = "persist_hide_enable" ]; then
        init_config
        read_json_val persist_hide_enable
    else
        [ -f "$SETTINGS_FILE" ] && grep -E "^${key}=" "$SETTINGS_FILE" | head -1 | cut -d'=' -f2-
    fi
}

write_setting() {
    local key="$1"
    local val="$2"
    if [ "$key" = "persist_hide_enable" ]; then
        init_config
        local pkgs_json
        pkgs_json=$(get_custom_packages | awk '{printf "\"" $0 "\","}' | sed 's/,$//')
        save_config "$val" "$pkgs_json"
    else
        [ ! -f "$SETTINGS_FILE" ] && touch "$SETTINGS_FILE"
        if grep -q "^${key}=" "$SETTINGS_FILE" 2>/dev/null; then
            sed -i "s|^${key}=.*|${key}=${val}|" "$SETTINGS_FILE"
        else
            echo "${key}=${val}" >> "$SETTINGS_FILE"
        fi
    fi
}

log() {
    log_json "$1"
}

build_regex() {
    get_all_packages | sed 's/\./\\./g' | tr '\n' '|' | sed 's/|$//'
}

do_hide() {
    log "do_hide: 开始执行挂载隐藏"
    [ ! -d "$TARGET_DIR" ] && { log "do_hide: TARGET_DIR 不存在"; return 1; }

    rm -rf "$EMPTY_DIR" 2>/dev/null
    mkdir -p "$EMPTY_DIR"

    local td_uid td_gid td_ctx
    td_uid=$(stat -c "%u" "$TARGET_DIR" 2>/dev/null)
    td_gid=$(stat -c "%g" "$TARGET_DIR" 2>/dev/null)
    td_ctx=$(stat -c "%C" "$TARGET_DIR" 2>/dev/null)
    [ -n "$td_uid" ] && [ -n "$td_gid" ] && chown "${td_uid}:${td_gid}" "$EMPTY_DIR" 2>/dev/null
    [ -n "$td_ctx" ] && chcon "$td_ctx" "$EMPTY_DIR" 2>/dev/null

    local orig name empty orig_uid orig_gid orig_ctx
    for orig in "$TARGET_DIR"/*; do
        [ -e "$orig" ] || continue
        name="${orig##*/}"
        # ===== 安全保护：以下系统关键目录绝对不 bind =====
        # 涉及锁屏密码 / 指纹 / 人脸 / 密钥库 / DRM / 出厂校准 等敏感数据
        # 一旦 bind 到空目录后异常断电，可能导致系统密钥失效、锁屏密码无法验证
        case "$name" in
            Dd*|Hd*) log "do_hide: 跳过 $name (Dd/Hd 前缀)"; continue ;;
            data_keystore*|keystore*|keymaster*|keymint*)
                log "do_hide: 保护跳过 $name (密钥库)"; continue ;;
            fingerprint*|biometric*|face*|gatekeeper*|weaver*)
                log "do_hide: 保护跳过 $name (生物识别/锁屏)"; continue ;;
            cred*|credential*|owner_info*|key_attestation*)
                log "do_hide: 保护跳过 $name (系统凭证)"; continue ;;
            playready*|widevine*|drm*|wv*|pr*|hdcp*)
                log "do_hide: 保护跳过 $name (DRM 密钥)"; continue ;;
            factory*|sec_storage*|secro*|trim_area*|tee*)
                log "do_hide: 保护跳过 $name (出厂校准/TEE)"; continue ;;
            time*|timekeep*|rfs*|modem*|nv*)
                log "do_hide: 保护跳过 $name (基带/校准)"; continue ;;
            audio*|sensor*|camera*|display*|battery*)
                log "do_hide: 保护跳过 $name (硬件校准)"; continue ;;
        esac

        empty="$EMPTY_DIR/$name"
        if [ -d "$orig" ]; then
            mkdir -p "$empty"
        else
            touch "$empty"
        fi

        orig_uid=$(stat -c "%u" "$orig" 2>/dev/null)
        orig_gid=$(stat -c "%g" "$orig" 2>/dev/null)
        orig_ctx=$(stat -c "%C" "$orig" 2>/dev/null)
        [ -n "$orig_uid" ] && [ -n "$orig_gid" ] && chown "${orig_uid}:${orig_gid}" "$empty" 2>/dev/null
        [ -n "$orig_ctx" ] && chcon "$orig_ctx" "$empty" 2>/dev/null

        if mount --bind --make-private "$empty" "$orig" 2>/dev/null; then
            echo "$orig" >> "$MOUNT_LIST"
            log "do_hide: 挂载成功 $name"
        else
            log "do_hide: 挂载失败 $name"
        fi
    done
    log "do_hide: 完成"
}

do_restore() {
    if [ -f "$MOUNT_LIST" ]; then
        local item
        while IFS= read -r item; do
            [ -n "$item" ] && umount -l "$item" 2>/dev/null && log "do_restore: 卸载 $item"
        done < "$MOUNT_LIST"
        rm -f "$MOUNT_LIST"
    fi
    rm -rf "$EMPTY_DIR" 2>/dev/null
    log "do_restore: 完成"
}

is_running() {
    [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE" 2>/dev/null)" 2>/dev/null
}

run_monitor() {
    local regex
    regex=$(build_regex)
    log "===== persist-hide 监听启动 PID=$$ regex=$regex ====="

    trap 'do_restore; rm -f "$PID_FILE"; exit 0' HUP TERM INT EXIT

    while true; do
        logcat -c 2>/dev/null
        logcat -v brief ActivityManager:I '*:S' 2>/dev/null | while IFS= read -r line; do
            if echo "$line" | grep -qE "Start proc.*($regex)"; then
                if [ ! -s "$MOUNT_LIST" ]; then
                    log "监听: 游戏启动，执行隐藏"
                    do_hide
                fi
            fi
            if echo "$line" | grep -qE "Killing.*($regex)|has died.*($regex)"; then
                if [ -s "$MOUNT_LIST" ]; then
                    log "监听: 游戏退出，解除挂载"
                    do_restore
                fi
            fi
        done
        log "logcat 退出，5 秒后重连"
        sleep 5
    done
}

cmd_status() {
    local enabled running pid mounted target_ok target_items
    enabled=$(read_setting persist_hide_enable)
    [ -z "$enabled" ] && enabled=0
    if is_running; then
        running=true
        pid=$(cat "$PID_FILE" 2>/dev/null)
    else
        running=false
        pid=""
    fi
    if [ -f "$MOUNT_LIST" ]; then
        mounted=$(wc -l < "$MOUNT_LIST" 2>/dev/null)
        mounted=${mounted:-0}
    else
        mounted=0
    fi
    if [ -d "$TARGET_DIR" ]; then
        target_ok=true
        target_items=$(ls -1A "$TARGET_DIR" 2>/dev/null | wc -l)
        target_items=${target_items:-0}
    else
        target_ok=false
        target_items=0
    fi
    printf '{"ok":true,"enabled":"%s","running":%s,"pid":"%s","mounted":%d,"target_ok":%s,"target":"%s","target_items":%d}\n' \
        "$enabled" "$running" "$pid" "$mounted" "$target_ok" "$TARGET_DIR" "$target_items"
}

cmd_list() {
    init_config
    local total=0 installed=0 pkg_json
    pkg_json=""
    local sep=""
    local is_installed is_custom

    while IFS= read -r pkg; do
        [ -z "$pkg" ] && continue
        total=$((total + 1))
        if pm path "$pkg" >/dev/null 2>&1; then
            is_installed=true
            installed=$((installed + 1))
        else
            is_installed=false
        fi
        if echo "$PKG_LIST_BUILTIN" | grep -qx "$pkg"; then
            is_custom=false
        else
            is_custom=true
        fi
        pkg_json="${pkg_json}${sep}{\"pkg\":\"${pkg}\",\"installed\":${is_installed},\"custom\":${is_custom}}"
        sep=","
    done <<PKGEOF
$(get_all_packages)
PKGEOF

    printf '{"ok":true,"target":"%s","packages":[%s]}\n' "$TARGET_DIR" "$pkg_json"
}

cmd_start() {
    if is_running; then
        pid=$(cat "$PID_FILE" 2>/dev/null)
        printf '{"ok":true,"msg":"已在运行","already":true,"pid":"%s"}\n' "$pid"
        return
    fi
    mkdir -p "$STATE_DIR"
    nohup sh "$MOD_DIR/scripts/persist-hide.sh" _monitor >/dev/null 2>&1 &
    sleep 1
    if is_running; then
        pid=$(cat "$PID_FILE" 2>/dev/null)
        printf '{"ok":true,"msg":"监听已启动","pid":"%s"}\n' "$pid"
    else
        echo '{"ok":false,"error":"启动失败，查看日志"}'
    fi
}

cmd_stop() {
    if is_running; then
        local pid
        pid=$(cat "$PID_FILE" 2>/dev/null)
        kill "$pid" 2>/dev/null
    fi
    do_restore
    rm -f "$PID_FILE"
    echo '{"ok":true,"msg":"监听已停止"}'
}

cmd_enable() {
    write_setting persist_hide_enable 1
    cmd_start
}

cmd_disable() {
    write_setting persist_hide_enable 0
    cmd_stop
}

cmd_restore() {
    do_restore
    echo '{"ok":true,"msg":"挂载已解除"}'
}

cmd_boot_apply() {
    local enabled
    enabled=$(read_setting persist_hide_enable)
    if [ "$enabled" = "1" ]; then
        cmd_start >/dev/null 2>&1
    fi
}

cmd_monitor() {
    mkdir -p "$STATE_DIR"
    echo "$$" > "$PID_FILE"
    run_monitor
}

cmd_add_pkg() {
    local pkg="$2"
    if [ -z "$pkg" ]; then
        echo '{"ok":false,"error":"missing package name"}'
        exit 1
    fi
    if ! echo "$pkg" | grep -qE '^[a-zA-Z][a-zA-Z0-9._]*$'; then
        echo '{"ok":false,"error":"包名格式无效"}'
        exit 1
    fi
    add_custom_pkg "$pkg"
}

cmd_remove_pkg() {
    local pkg="$2"
    if [ -z "$pkg" ]; then
        echo '{"ok":false,"error":"missing package name"}'
        exit 1
    fi
    remove_custom_pkg "$pkg"
}

cmd_get_config() {
    get_config_json
}

case "$ACTION" in
    status)      cmd_status ;;
    list)        cmd_list ;;
    start)       cmd_start ;;
    stop)        cmd_stop ;;
    enable)      cmd_enable ;;
    disable)     cmd_disable ;;
    restore)     cmd_restore ;;
    boot_apply)  cmd_boot_apply ;;
    _monitor)    cmd_monitor ;;
    add_pkg)     cmd_add_pkg "$@" ;;
    remove_pkg)  cmd_remove_pkg "$@" ;;
    get_config)  cmd_get_config ;;
    *)
        echo '{"ok":false,"error":"unknown action"}'
        exit 1 ;;
esac


