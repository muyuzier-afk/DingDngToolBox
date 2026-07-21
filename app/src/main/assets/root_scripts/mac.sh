#!/system/bin/sh
# MAC 地址随机化（WiFi + 蓝牙）
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 用法:
#   sh mac.sh info      查看当前 WiFi / 蓝牙 MAC（JSON）
#   sh mac.sh random    随机化 WiFi + 蓝牙 MAC（本地管理位 02:）
#   sh mac.sh restore   恢复蓝牙 MAC（WiFi MAC 重启后自动恢复）

MOD_DIR="/data/adb/ddj_toolbox"
SETTINGS_FILE="$MOD_DIR/settings.conf"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "mac"
PATH="$PATH:/system/bin:/system/xbin:/data/adb/ksu/bin:/data/adb/magisk"
ACTION="$1"

log() { log_json "$1"; }

read_setting() {
    local key="$1"
    [ -f "$SETTINGS_FILE" ] && grep -E "^${key}=" "$SETTINGS_FILE" | head -1 | cut -d'=' -f2-
}
write_setting() {
    local key="$1"; local val="$2"
    [ ! -f "$SETTINGS_FILE" ] && touch "$SETTINGS_FILE"
    if grep -q "^${key}=" "$SETTINGS_FILE" 2>/dev/null; then
        sed -i "s|^${key}=.*|${key}=${val}|" "$SETTINGS_FILE"
    else
        echo "${key}=${val}" >> "$SETTINGS_FILE"
    fi
}

# 找第一个存在的 wlan 接口
wlan_if() {
    for i in wlan0 wlan1; do
        [ -d "/sys/class/net/$i" ] && { echo "$i"; return; }
    done
}
read_if_mac() {
    local i="$1"
    [ -z "$i" ] && return
    cat "/sys/class/net/$i/address" 2>/dev/null | tr 'A-F' 'a-f'
}
read_bt_mac() {
    settings get secure bluetooth_address 2>/dev/null | tr -d ' \n\r'
}
# 随机本地管理 MAC；$1=printf 格式(%02x 小写 / %02X 大写)
gen_mac() {
    printf "02:$1:$1:$1:$1:$1" \
        $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256))
}

cmd_info() {
    local wif=$(wlan_if)
    local wmac=$(read_if_mac "$wif"); [ -z "$wmac" ] && wmac="-"
    local bt=$(read_bt_mac); { [ -z "$bt" ] || [ "$bt" = "null" ]; } && bt="-"
    local orig_bt=$(read_setting orig_bt_mac); [ -z "$orig_bt" ] && orig_bt="$bt"
    local modified="false"
    [ "$bt" != "-" ] && [ "$bt" != "$orig_bt" ] && modified="true"
    printf '{"ok":true,"iface":"%s","wifi_mac":"%s","bt_mac":"%s","orig_bt_mac":"%s","modified":%s}\n' \
        "${wif:-未找到}" "$wmac" "$bt" "$orig_bt" "$modified"
}

cmd_random() {
    # ── WiFi MAC ──
    local wif=$(wlan_if)
    local wifi_result="skipped"; local wifi_mac="-"
    if [ -n "$wif" ] && command -v ip >/dev/null 2>&1; then
        local m=$(gen_mac '%02x')
        ip link set "$wif" down 2>/dev/null
        if ip link set "$wif" address "$m" 2>/dev/null; then
            ip link set "$wif" up 2>/dev/null
            sleep 1
            if [ "$(read_if_mac "$wif")" = "$m" ]; then
                wifi_result="success"; wifi_mac="$m"
            else
                wifi_result="failed"; wifi_mac="$(read_if_mac "$wif")"
            fi
        else
            ip link set "$wif" up 2>/dev/null
            wifi_result="failed"
        fi
    fi
    # ── 蓝牙 MAC ──（首次备份原值）
    if [ -z "$(read_setting orig_bt_mac)" ]; then
        local ob=$(read_bt_mac)
        [ -n "$ob" ] && [ "$ob" != "null" ] && write_setting orig_bt_mac "$ob"
    fi
    local newbt=$(gen_mac '%02X')
    local bt_result="failed"; local bt_mac="-"
    settings put secure bluetooth_address "$newbt" 2>/dev/null
    if [ "$(read_bt_mac)" = "$newbt" ]; then
        bt_result="success"; bt_mac="$newbt"
    fi
    log "random wifi=$wifi_result bt=$bt_result"
    printf '{"ok":true,"msg":"MAC 随机化完成","wifi":"%s","wifi_mac":"%s","bt":"%s","bt_mac":"%s"}\n' \
        "$wifi_result" "$wifi_mac" "$bt_result" "$bt_mac"
}

cmd_restore() {
    local ob=$(read_setting orig_bt_mac)
    if [ -n "$ob" ]; then
        settings put secure bluetooth_address "$ob" 2>/dev/null
        log "restore bt=$ob"
        printf '{"ok":true,"msg":"蓝牙 MAC 已恢复，WiFi MAC 重启后自动恢复","bt_mac":"%s"}\n' "$ob"
    else
        printf '{"ok":true,"msg":"无蓝牙备份；WiFi MAC 重启后自动恢复"}\n'
    fi
}

case "$ACTION" in
    info)    cmd_info ;;
    random)  cmd_random ;;
    restore) cmd_restore ;;
    *)
        echo '{"ok":false,"error":"unknown action - use: info | random | restore"}'
        exit 1 ;;
esac
