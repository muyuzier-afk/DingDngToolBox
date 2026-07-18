#!/system/bin/sh
# 机型伪装脚本
# 作者: 呆呆 | QQ:891354018 | 群:774886621

MOD_DIR="/data/adb/ddj_toolbox"
SETTINGS_FILE="$MOD_DIR/settings.conf"
PROP_FILE="$MOD_DIR/system.prop"
PRESETS_CACHE="$MOD_DIR/presets-cache.json"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "spoof"
ACTION="$1"
MODEL_ID="$2"

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

backup_orig_once() {
    if [ -z "$(read_setting 'orig_model')" ]; then
        write_setting "orig_model" "$(getprop ro.product.model)"
        write_setting "orig_brand" "$(getprop ro.product.brand)"
        write_setting "orig_manufacturer" "$(getprop ro.product.manufacturer)"
        write_setting "orig_device" "$(getprop ro.product.device)"
        write_setting "orig_name" "$(getprop ro.product.name)"
        write_setting "orig_marketname" "$(getprop ro.product.marketname)"
    fi
}

# 从 presets.json 提取指定 id 的所有 props，写入 system.prop
write_prop_from_preset() {
    local id="$1"

    [ ! -f "$PRESETS_CACHE" ] && {
        log_json "cache 不存在"
        return 1
    }

    > "$PROP_FILE"

    # awk 状态机：
    # state=0 还没找到目标 id
    # state=1 找到目标 id，等待 "props": {
    # state=2 在 props 块内，提取 key:value
    awk -v target="$id" '
    BEGIN { state=0 }

    state==0 {
        line = $0
        if (match(line, /"id"[[:space:]]*:[[:space:]]*"[^"]+"/)) {
            kv = substr(line, RSTART, RLENGTH)
            sub(/^"id"[[:space:]]*:[[:space:]]*"/, "", kv)
            sub(/"$/, "", kv)
            if (kv == target) state = 1
        }
        next
    }

    state==1 {
        if (index($0, "\"props\"") > 0 && index($0, "{") > 0) {
            state = 2
        }
        next
    }

    state==2 {
        # 退出条件：单独一行的 } 或 },
        line_trim = $0
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", line_trim)
        if (line_trim == "}" || line_trim == "},") {
            exit
        }

        # 匹配 "key": "value"（value 不能含未转义引号）
        line = $0
        # 提取 key
        if (!match(line, /"[^"]+"[[:space:]]*:[[:space:]]*"/)) next
        keypart = substr(line, RSTART, RLENGTH)
        sub(/^"/, "", keypart)
        sub(/"[[:space:]]*:[[:space:]]*"$/, "", keypart)

        # 提取 value（去掉 key:" 前缀，再去掉尾部 ", 或 "）
        val = line
        sub(/^[^"]*"[^"]*"[[:space:]]*:[[:space:]]*"/, "", val)
        sub(/"[[:space:]]*,?[[:space:]]*$/, "", val)

        if (keypart != "" && val != "") {
            print keypart "=" val
        }
    }
    ' "$PRESETS_CACHE" >> "$PROP_FILE"

    chmod 644 "$PROP_FILE"
    local n=$(wc -l < "$PROP_FILE" 2>/dev/null)
    log_json "$id 写入 $n 条 prop"

    [ "${n:-0}" -ge 3 ]
}

# 从 cache 提取该 id 的 label/brand/model（用于状态显示）
extract_meta() {
    local id="$1"
    [ ! -f "$PRESETS_CACHE" ] && return

    awk -v target="$id" '
    BEGIN { state=0; lbl=""; brd=""; mdl="" }
    state==0 {
        line = $0
        if (match(line, /"id"[[:space:]]*:[[:space:]]*"[^"]+"/)) {
            kv = substr(line, RSTART, RLENGTH)
            sub(/^"id"[[:space:]]*:[[:space:]]*"/, "", kv)
            sub(/"$/, "", kv)
            if (kv == target) state = 1
        }
        next
    }
    state==1 {
        if (lbl == "" && /"label"[[:space:]]*:/) {
            line = $0
            sub(/.*"label"[[:space:]]*:[[:space:]]*"/, "", line)
            sub(/".*/, "", line)
            lbl = line
        }
        if (brd == "" && /"brand"[[:space:]]*:/ && !/system|vendor|odm|bootimage/) {
            line = $0
            sub(/.*"brand"[[:space:]]*:[[:space:]]*"/, "", line)
            sub(/".*/, "", line)
            brd = line
        }
        if (mdl == "" && /"model"[[:space:]]*:/ && !/system|vendor|odm|bootimage/) {
            line = $0
            sub(/.*"model"[[:space:]]*:[[:space:]]*"/, "", line)
            sub(/".*/, "", line)
            mdl = line
        }
        if (/"props"[[:space:]]*:/) {
            print lbl "|" brd "|" mdl
            exit
        }
    }
    ' "$PRESETS_CACHE"
}

# ============ apply ============
cmd_apply() {
    local id="$1"
    [ -z "$id" ] && { echo '{"ok":false,"error":"missing id"}'; exit 1; }

    backup_orig_once

    local meta=$(extract_meta "$id")
    local label=$(echo "$meta" | cut -d'|' -f1)
    local brand=$(echo "$meta" | cut -d'|' -f2)
    local model=$(echo "$meta" | cut -d'|' -f3)

    if write_prop_from_preset "$id"; then
        write_setting "current_spoof" "$id"
        write_setting "current_spoof_label" "$label"
        write_setting "current_spoof_brand" "$brand"
        write_setting "current_spoof_model" "$model"
        printf '{"ok":true,"id":"%s","label":"%s","brand":"%s","model":"%s","msg":"已应用 %s，重启后生效"}\n' \
            "$id" "$label" "$brand" "$model" "$label"
    else
        echo '{"ok":false,"error":"应用失败：未找到该机型 props 数据，请重新同步机型库"}'
        exit 1
    fi
}

# ============ restore ============
cmd_restore() {
    > "$PROP_FILE"
    write_setting "current_spoof" ""
    write_setting "current_spoof_label" ""
    write_setting "current_spoof_brand" ""
    write_setting "current_spoof_model" ""
    echo '{"ok":true,"msg":"已恢复原机型，重启后生效"}'
}

# ============ status ============
cmd_status() {
    local cur=$(read_setting current_spoof)
    local label=$(read_setting current_spoof_label)
    local brand=$(read_setting current_spoof_brand)
    local model=$(read_setting current_spoof_model)
    local orig_brand=$(read_setting orig_brand)
    local orig_model=$(read_setting orig_model)

    [ -z "$orig_brand" ] && orig_brand=$(getprop ro.product.brand)
    [ -z "$orig_model" ] && orig_model=$(getprop ro.product.model)

    if [ -n "$cur" ]; then
        printf '{"active":true,"id":"%s","label":"%s","brand":"%s","model":"%s","orig_brand":"%s","orig_model":"%s"}\n' \
            "$cur" "$label" "$brand" "$model" "$orig_brand" "$orig_model"
    else
        printf '{"active":false,"id":"","label":"原机型","brand":"%s","model":"%s","orig_brand":"%s","orig_model":"%s"}\n' \
            "$orig_brand" "$orig_model" "$orig_brand" "$orig_model"
    fi
}

cmd_boot_apply() {
    local cur=$(read_setting current_spoof)
    if [ -n "$cur" ]; then
        backup_orig_once
        write_prop_from_preset "$cur" >/dev/null 2>&1
    fi
}

case "$ACTION" in
    apply)       cmd_apply "$MODEL_ID" ;;
    restore)     cmd_restore ;;
    status)      cmd_status ;;
    current)     cmd_status ;;
    boot_apply)  cmd_boot_apply ;;
    *)
        echo '{"ok":false,"error":"unknown action"}'
        exit 1 ;;
esac

