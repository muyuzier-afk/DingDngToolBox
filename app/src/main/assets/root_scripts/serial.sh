#!/system/bin/sh
# 序列号管理脚本（resetprop）
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 用法:
#   sh serial.sh info               获取当前 / 原始序列号 + 全部 SN 属性（JSON）
#   sh serial.sh apply <sn>         应用自定义序列号
#   sh serial.sh random             随机生成 12 位序列号并应用
#   sh serial.sh restore            恢复原始出厂序列号
#   sh serial.sh boot_apply         开机静默重新应用（service.sh 调用）

MOD_DIR="/data/adb/ddj_toolbox"
SETTINGS_FILE="$MOD_DIR/settings.conf"
ACTION="$1"
ARG="$2"

. "$MOD_DIR/scripts/lib_log.sh"
init_log "serial"

# 兜底 PATH（KernelSU 把 resetprop 放在这里）
PATH="$PATH:/data/adb/ksu/bin:/data/adb/magisk"

log() {
    log_json "$1"
}

read_setting() {
    local key="$1"
    [ -f "$SETTINGS_FILE" ] && grep -E "^${key}=" "$SETTINGS_FILE" | head -1 | cut -d'=' -f2-
}

write_setting() {
    local key="$1"
    local val="$2"
    [ ! -f "$SETTINGS_FILE" ] && touch "$SETTINGS_FILE"
    if grep -q "^${key}=" "$SETTINGS_FILE" 2>/dev/null; then
        sed -i "s|^${key}=.*|${key}=${val}|" "$SETTINGS_FILE"
    else
        echo "${key}=${val}" >> "$SETTINGS_FILE"
    fi
}

# 仅在第一次时把当前真实序列号备份成 orig
backup_orig_once() {
    local saved=$(read_setting 'orig_serialno')
    if [ -z "$saved" ]; then
        local active=$(read_setting 'current_serial')
        local cur=$(getprop ro.serialno)
        # 如果当前已经被改过（启动时 boot_apply 抢先），用 settings 里记录的 active
        # 但 active 不是原始 — 此时无法回溯，放弃，仅在干净状态下备份
        if [ -z "$active" ] || [ "$active" != "$cur" ]; then
            write_setting "orig_serialno" "$cur"
        fi
    fi
}

apply_props() {
    local sn="$1"
    resetprop -n ro.serialno      "$sn" 2>/dev/null
    resetprop -n ro.boot.serialno "$sn" 2>/dev/null
    resetprop -n ro.serial        "$sn" 2>/dev/null
    resetprop -n ro.ril.oem.noser ""    2>/dev/null
}

cmd_info() {
    backup_orig_once
    local cur=$(getprop ro.serialno)
    local orig=$(read_setting orig_serialno)
    [ -z "$orig" ] && orig="$cur"
    local boot_sn=$(getprop ro.boot.serialno)
    local serial=$(getprop ro.serial)
    local android_id=$(settings get secure android_id 2>/dev/null)
    [ -z "$android_id" ] && android_id="-"

    local modified="false"
    [ "$cur" != "$orig" ] && modified="true"

    printf '{"ok":true,"current":"%s","orig":"%s","ro_serialno":"%s","ro_boot_serialno":"%s","ro_serial":"%s","android_id":"%s","modified":%s}\n' \
        "$cur" "$orig" "$cur" "$boot_sn" "$serial" "$android_id" "$modified"
}

cmd_apply() {
    local sn="$1"
    if [ -z "$sn" ]; then
        echo '{"ok":false,"error":"未提供序列号"}'
        return 1
    fi
    if ! command -v resetprop >/dev/null 2>&1; then
        echo '{"ok":false,"error":"resetprop 不可用，需 KernelSU/Magisk 环境"}'
        return 1
    fi

    backup_orig_once
    apply_props "$sn"
    sleep 1
    local cur=$(getprop ro.serialno)
    if [ "$cur" = "$sn" ]; then
        write_setting "current_serial" "$sn"
        log "应用 $sn 成功"
        printf '{"ok":true,"sn":"%s","msg":"序列号已生效：%s"}\n' "$sn" "$sn"
    else
        log "应用 $sn 失败：当前=$cur"
        echo '{"ok":false,"error":"resetprop 写入未生效"}'
    fi
}

cmd_random() {
    local sn=$(cat /dev/urandom 2>/dev/null | tr -dc 'A-Z0-9' | head -c 12)
    if [ -z "$sn" ] || [ ${#sn} -lt 8 ]; then
        sn=$(echo "$(date +%s%N)$$" | md5sum 2>/dev/null | tr 'a-f' 'A-F' | head -c 12)
    fi
    if [ -z "$sn" ] || [ ${#sn} -lt 8 ]; then
        echo '{"ok":false,"error":"无法生成随机序列号"}'
        return 1
    fi
    cmd_apply "$sn"
}

cmd_restore() {
    local orig=$(read_setting orig_serialno)
    if [ -z "$orig" ]; then
        echo '{"ok":false,"error":"未找到原始序列号备份"}'
        return 1
    fi
    if ! command -v resetprop >/dev/null 2>&1; then
        echo '{"ok":false,"error":"resetprop 不可用"}'
        return 1
    fi

    apply_props "$orig"
    sleep 1
    local cur=$(getprop ro.serialno)
    if [ "$cur" = "$orig" ]; then
        write_setting "current_serial" ""
        log "恢复原始 $orig"
        printf '{"ok":true,"sn":"%s","msg":"已恢复原始序列号"}\n' "$orig"
    else
        echo '{"ok":false,"error":"恢复失败"}'
    fi
}

cmd_boot_apply() {
    local sn=$(read_setting current_serial)
    if [ -n "$sn" ] && command -v resetprop >/dev/null 2>&1; then
        backup_orig_once
        apply_props "$sn"
        log "boot_apply $sn"
    fi
}

case "$ACTION" in
    info)       cmd_info ;;
    apply)      cmd_apply "$ARG" ;;
    random)     cmd_random ;;
    restore)    cmd_restore ;;
    boot_apply) cmd_boot_apply ;;
    *)
        echo '{"ok":false,"error":"unknown action - use: info | apply <sn> | random | restore | boot_apply"}'
        exit 1 ;;
esac
