#!/system/bin/sh
# 机型预设管理脚本 (本地版 - 已剥离云端同步)
# 原作者: 呆呆 | 本地化提取: MocoLab
# 用法:
#   sh presets.sh import <file>   从本地 JSON 文件导入机型库
#   sh presets.sh list            列出所有预设 (读取本地缓存)
#   sh presets.sh info            查询缓存信息
#
# 机型库格式: 标准 presets.json (含 presets 数组, 每项有 id/label/brand/model/props)
# 可从 https://gitee.com/island-style/daoshi/raw/master/presets.json 手动下载后导入

MOD_DIR="/data/adb/ddj_toolbox"
CACHE_FILE="$MOD_DIR/presets-cache.json"

. "$MOD_DIR/scripts/lib_log.sh"
init_log "presets"

ACTION="$1"
IMPORT_FILE="$2"

log() {
    log_json "$1"
}

# 从本地 JSON 文件导入机型库
cmd_import() {
    local src="$IMPORT_FILE"
    if [ -z "$src" ]; then
        echo '{"ok":false,"error":"缺少文件路径，用法: presets.sh import <file>"}'
        return
    fi
    if [ ! -f "$src" ]; then
        echo '{"ok":false,"error":"文件不存在: '"$src"'"}'
        return
    fi
    if ! grep -qE '"presets"|"id"' "$src"; then
        echo '{"ok":false,"error":"文件格式错误，需包含 presets 或 id 字段"}'
        return
    fi

    cp "$src" "$CACHE_FILE"
    chmod 644 "$CACHE_FILE"

    local version=$(grep -oE '"version"[[:space:]]*:[[:space:]]*"[^"]+"' "$CACHE_FILE" | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
    local updated=$(grep -oE '"updated"[[:space:]]*:[[:space:]]*"[^"]+"' "$CACHE_FILE" | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
    local count=$(grep -oE '"id"[[:space:]]*:' "$CACHE_FILE" | wc -l)

    log "本地导入成功: $src ($count 条)"
    printf '{"ok":true,"msg":"导入成功","version":"%s","updated":"%s","count":%d}\n' \
        "$version" "$updated" "$count"
}

cmd_list() {
    if [ ! -f "$CACHE_FILE" ]; then
        echo '{"ok":false,"need_sync":true,"hint":"请使用 presets.sh import <file> 导入机型库"}'
        return
    fi

    local tmp_filtered="/data/local/tmp/presets_filt_$$.txt"
    local tmp_output="/data/local/tmp/presets_out_$$.txt"

    grep -nE '"(id|category|label|brand|model|android|props)"[[:space:]]*:' "$CACHE_FILE" > "$tmp_filtered" 2>/dev/null

    awk -F: '
    BEGIN {
        cur_id = ""; cur_cat = ""; cur_label = ""
        cur_brand = ""; cur_model = ""; cur_android = ""
        first = 1
        printf "{\"ok\":true,\"presets\":["
    }

    function emit() {
        if (cur_id != "") {
            if (first) { first = 0 } else { printf "," }
            printf "{\"id\":\"%s\",\"category\":\"%s\",\"label\":\"%s\",\"brand\":\"%s\",\"model\":\"%s\",\"android\":\"%s\"}", \
                cur_id, cur_cat, cur_label, cur_brand, cur_model, cur_android
        }
    }

    function get_value(line) {
        v = line
        sub(/^[0-9]+:/, "", v)
        sub(/^[^:]*:[[:space:]]*"/, "", v)
        sub(/"[[:space:]]*,?[[:space:]]*$/, "", v)
        return v
    }

    /"id"[[:space:]]*:[[:space:]]*"/ {
        emit()
        cur_id = get_value($0)
        cur_cat = ""; cur_label = ""; cur_brand = ""; cur_model = ""; cur_android = ""
        next
    }

    cur_id == "" { next }

    /"category"[[:space:]]*:/ && cur_cat == "" { cur_cat = get_value($0); next }
    /"label"[[:space:]]*:/ && cur_label == "" { cur_label = get_value($0); next }
    /"brand"[[:space:]]*:/ && cur_brand == "" {
        if (index($0, ".brand") == 0) { cur_brand = get_value($0) }
        next
    }
    /"model"[[:space:]]*:/ && cur_model == "" {
        if (index($0, ".model") == 0) { cur_model = get_value($0) }
        next
    }
    /"android"[[:space:]]*:/ && cur_android == "" { cur_android = get_value($0); next }
    /"props"[[:space:]]*:/ { emit(); cur_id = "" }

    END { emit(); printf "]}" }
    ' "$tmp_filtered" > "$tmp_output"

    cat "$tmp_output"
    rm -f "$tmp_filtered" "$tmp_output"
}

cmd_info() {
    if [ ! -f "$CACHE_FILE" ]; then
        echo '{"ok":true,"cached":false,"version":"","updated":"","count":0,"hint":"请使用 presets.sh import <file> 导入机型库"}'
        return
    fi

    local version=$(grep -oE '"version"[[:space:]]*:[[:space:]]*"[^"]+"' "$CACHE_FILE" | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
    local updated=$(grep -oE '"updated"[[:space:]]*:[[:space:]]*"[^"]+"' "$CACHE_FILE" | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
    local count=$(grep -oE '"id"[[:space:]]*:' "$CACHE_FILE" | wc -l)

    printf '{"ok":true,"cached":true,"version":"%s","updated":"%s","count":%d}\n' \
        "$version" "$updated" "$count"
}

case "$ACTION" in
    import)  cmd_import ;;
    list)    cmd_list ;;
    info)    cmd_info ;;
    *)
        echo '{"ok":false,"error":"unknown action - use: import <file> | list | info"}'
        exit 1 ;;
esac
