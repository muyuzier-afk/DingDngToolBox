#!/system/bin/sh
# Android ID 修改脚本
# 作者: 呆呆 | QQ:891354018 | 群:774886621
#
# 注意:
#   - 系统级 Android ID = settings get secure android_id
#   - Android 8.0+ 后游戏普遍用 SSAID（每应用独立），改这个对反作弊用处不大
#   - 改系统 Android ID 会导致很多应用要求"重新登录"
#
# 用法:
#   sh android_id.sh status                  查看当前 Android ID
#   sh android_id.sh set <16位hex>           设置自定义 Android ID
#   sh android_id.sh random                  随机生成新 Android ID
#   sh android_id.sh restore                 恢复原始（清除会重新生成）

MOD_DIR="/data/adb/ddj_toolbox"
LOG_FILE="/data/local/tmp/webui_android_id.log"
BACKUP_FILE="/data/adb/webui_android_id_backup"
SETTINGS_DB="/data/system/users/0/settings_secure.xml"

ACTION="$1"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# 验证 Android ID 格式（必须是 16 位十六进制）
validate_id() {
    local id="$1"
    [ -z "$id" ] && return 1
    case "$id" in
        *[!0-9a-fA-F]*) return 1 ;;
    esac
    local len=${#id}
    [ "$len" -ne 16 ] && return 1
    return 0
}

# 生成随机 Android ID（16 位十六进制）
gen_random_id() {
    # 用 /dev/urandom 取 8 字节，转成 16 位 hex
    head -c 8 /dev/urandom 2>/dev/null | od -An -tx1 | tr -d ' \n' | head -c 16
}

# 备份当前 Android ID
backup_current() {
    local cur
    cur=$(settings get secure android_id 2>/dev/null | tr -d ' \n\r')
    if [ -n "$cur" ] && [ "$cur" != "null" ]; then
        if [ ! -f "$BACKUP_FILE" ]; then
            echo "$cur" > "$BACKUP_FILE"
            log "已备份原始 Android ID: $cur"
        fi
    fi
}

cmd_status() {
    local cur backup modified
    cur=$(settings get secure android_id 2>/dev/null | tr -d ' \n\r')
    [ -z "$cur" ] || [ "$cur" = "null" ] && cur="(未设置)"

    if [ -f "$BACKUP_FILE" ]; then
        backup=$(cat "$BACKUP_FILE" 2>/dev/null | tr -d ' \n\r')
    else
        backup=""
    fi

    if [ -n "$backup" ] && [ "$cur" != "$backup" ]; then
        modified="true"
    else
        modified="false"
    fi

    printf '{"ok":true,"current":"%s","original":"%s","modified":%s}\n' \
        "$cur" "$backup" "$modified"
}

cmd_set() {
    local new_id="$2"

    if ! validate_id "$new_id"; then
        echo '{"ok":false,"error":"Android ID 必须是 16 位十六进制（0-9 a-f）"}'
        return 1
    fi

    # 备份
    backup_current

    # 转为小写
    new_id=$(echo "$new_id" | tr '[:upper:]' '[:lower:]')

    # 修改
    settings put secure android_id "$new_id" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo '{"ok":false,"error":"修改失败，需要 root 权限"}'
        return 1
    fi

    # 验证
    local cur
    cur=$(settings get secure android_id 2>/dev/null | tr -d ' \n\r')
    if [ "$cur" = "$new_id" ]; then
        log "已设置新 Android ID: $new_id"
        printf '{"ok":true,"msg":"已设置","android_id":"%s"}\n' "$new_id"
    else
        printf '{"ok":false,"error":"设置失败，当前值：%s"}\n' "$cur"
    fi
}

cmd_random() {
    local new_id
    new_id=$(gen_random_id)

    if ! validate_id "$new_id"; then
        echo '{"ok":false,"error":"随机生成失败"}'
        return 1
    fi

    backup_current

    settings put secure android_id "$new_id" 2>/dev/null

    local cur
    cur=$(settings get secure android_id 2>/dev/null | tr -d ' \n\r')
    if [ "$cur" = "$new_id" ]; then
        log "随机设置新 Android ID: $new_id"
        printf '{"ok":true,"msg":"已随机生成","android_id":"%s"}\n' "$new_id"
    else
        echo '{"ok":false,"error":"设置失败"}'
    fi
}

cmd_restore() {
    if [ ! -f "$BACKUP_FILE" ]; then
        echo '{"ok":false,"error":"没有备份，无法恢复"}'
        return 1
    fi

    local backup
    backup=$(cat "$BACKUP_FILE" 2>/dev/null | tr -d ' \n\r')
    if [ -z "$backup" ]; then
        echo '{"ok":false,"error":"备份为空"}'
        return 1
    fi

    settings put secure android_id "$backup" 2>/dev/null

    local cur
    cur=$(settings get secure android_id 2>/dev/null | tr -d ' \n\r')
    if [ "$cur" = "$backup" ]; then
        log "已恢复原始 Android ID: $backup"
        printf '{"ok":true,"msg":"已恢复原始","android_id":"%s"}\n' "$backup"
    else
        echo '{"ok":false,"error":"恢复失败"}'
    fi
}

case "$ACTION" in
    status)  cmd_status ;;
    set)     cmd_set "$@" ;;
    random)  cmd_random ;;
    restore) cmd_restore ;;
    *)
        echo '{"ok":false,"error":"unknown action - use: status | set <16hex> | random | restore"}'
        exit 1 ;;
esac
