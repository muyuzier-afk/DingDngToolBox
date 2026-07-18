#!/system/bin/sh
# OAID + SSAID 高级重置脚本
# 作者: 呆呆 | QQ:891354018 | 群:774886621

MOD_DIR="/data/adb/ddj_toolbox"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "oaid"
OAID_FILE="/data/system/oaid_persistence_0"
SSAID_FILE="/data/system/users/0/settings_ssaid.xml"
USER0_DIR="/data/system/users/0"
PATH="$PATH:/data/adb/ksu/bin:/data/adb/magisk:/system/bin:/system/xbin"
ACTION="$1"

log() {
    log_json "$1"
}

is_lenovo() {
    local mfr brand model
    mfr=$(getprop ro.product.manufacturer 2>/dev/null | tr -d '"\\')
    brand=$(getprop ro.product.brand 2>/dev/null | tr -d '"\\')
    model=$(getprop ro.product.model 2>/dev/null | tr -d '"\\')
    case "$mfr$brand$model" in
        *[Ll]enovo*|*联想*|*[Mm]otorola*|*moto*) return 0 ;;
    esac
    return 1
}

gen_random() {
    local val
    val=$(cat /dev/urandom 2>/dev/null | tr -dc 'a-f0-9' | head -c 18)
    if [ -z "$val" ] || [ ${#val} -lt 18 ]; then
        val=$(date +%s%N 2>/dev/null | md5sum 2>/dev/null | head -c 18)
    fi
    if [ -z "$val" ] || [ ${#val} -lt 18 ]; then
        val=$(date +%s | md5sum 2>/dev/null | head -c 18)
    fi
    printf '%s' "$val"
}

write_oaid() {
    local val="$1"
    { printf '\000\020'; printf '%s' "$val"; } > "$OAID_FILE" 2>/dev/null
}

read_oaid() {
    tail -c +3 "$OAID_FILE" 2>/dev/null
}

read_uuid() {
    [ -f "$SSAID_FILE" ] || return
    local tmp="/data/local/tmp/_ss_$$.xml"
    cp "$SSAID_FILE" "$tmp" 2>/dev/null || return
    abx2xml -i "$tmp" 2>/dev/null
    local uuid
    uuid=$(grep 'userkey' "$tmp" 2>/dev/null | awk -F'"' '{print $6}' | head -1)
    rm -f "$tmp" 2>/dev/null
    printf '%s' "$uuid"
}

cmd_info() {
    local mfr brand model oaid uuid oaid_ok lenovo_bool
    mfr=$(getprop ro.product.manufacturer 2>/dev/null | tr -d '"\\')
    brand=$(getprop ro.product.brand 2>/dev/null | tr -d '"\\')
    model=$(getprop ro.product.model 2>/dev/null | tr -d '"\\')
    [ -z "$mfr" ] && mfr="-"
    [ -z "$brand" ] && brand="-"
    [ -z "$model" ] && model="-"
    oaid=$(read_oaid)
    [ -z "$oaid" ] && oaid="-"
    uuid=$(read_uuid)
    [ -z "$uuid" ] && uuid="-"
    if [ -f "$OAID_FILE" ]; then oaid_ok=true; else oaid_ok=false; fi
    if is_lenovo; then lenovo_bool=true; else lenovo_bool=false; fi
    printf '{"ok":true,"oaid":"%s","uuid":"%s","manufacturer":"%s","brand":"%s","model":"%s","lenovo":%s,"oaid_file_ok":%s}\n' \
        "$oaid" "$uuid" "$mfr" "$brand" "$model" "$lenovo_bool" "$oaid_ok"
}

cmd_random_oaid() {
    if [ ! -f "$OAID_FILE" ]; then
        printf '{"ok":false,"error":"OAID 文件不存在，本机不支持"}\n'
        exit 1
    fi
    local new
    new=$(gen_random)
    write_oaid "$new"
    log "random_oaid: 新 OAID=$new"
    printf '{"ok":true,"oaid":"%s","msg":"OAID 已重置：%s"}\n' "$new" "$new"
}

cmd_wipe_partial() {
    local new
    new=$(gen_random)
    [ -f "$OAID_FILE" ] && write_oaid "$new"
    rm -f "$SSAID_FILE" 2>/dev/null
    rm -rf "$USER0_DIR/registered_services/" 2>/dev/null
    rm -f "$USER0_DIR/app_idle_stats.xml" 2>/dev/null
    log "wipe_partial: 新 OAID=$new SSAID 已删除"
    printf '{"ok":true,"oaid":"%s","msg":"局部重置完成，3 秒后重启","reboot":true}\n' "$new"
    sleep 3
    reboot
}

cmd_wipe_global() {
    local new
    new=$(gen_random)
    [ -f "$OAID_FILE" ] && write_oaid "$new"
    rm -rf "$USER0_DIR/" 2>/dev/null
    log "wipe_global: 新 OAID=$new users/0 已清空"
    printf '{"ok":true,"oaid":"%s","msg":"全局重置完成，3 秒后重启","reboot":true}\n' "$new"
    sleep 3
    reboot
}

case "$ACTION" in
    info)         cmd_info ;;
    random_oaid)  cmd_random_oaid ;;
    wipe_partial) cmd_wipe_partial ;;
    wipe_global)  cmd_wipe_global ;;
    *)
        printf '{"ok":false,"error":"unknown action"}\n'
        exit 1 ;;
esac
