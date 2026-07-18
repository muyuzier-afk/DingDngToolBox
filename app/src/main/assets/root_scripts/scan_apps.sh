#!/system/bin/sh
# 应用列表扫描脚本（使用内置 aapt2）
# 作者: 呆呆 | QQ:891354018 | 群:774886621

MOD_DIR="/data/adb/ddj_toolbox"
CACHE_FILE="$MOD_DIR/apps-cache.json"
ICON_DIR="$MOD_DIR/webroot/icons"
PROGRESS_FILE="/data/local/tmp/webui_scan_progress.txt"
SCAN_LOCK="/data/local/tmp/webui_scan.lock"
SSAID_FILE="/data/system/users/0/settings_ssaid.xml"

# 内置 aapt2 路径
AAPT2="$MOD_DIR/aapt2/aapt2"

ACTION="$1"

. "$MOD_DIR/scripts/lib_log.sh"
init_log "scan"

log() {
    log_json "$1"
}

# ============ status ============
if [ "$ACTION" = "status" ]; then
    if [ -f "$SCAN_LOCK" ]; then
        if [ -f "$PROGRESS_FILE" ]; then
            cat "$PROGRESS_FILE"
        else
            echo '{"scanning":true,"current":0,"total":0}'
        fi
    elif [ -f "$CACHE_FILE" ]; then
        count=$(grep -oE '"pkg"' "$CACHE_FILE" | wc -l)
        time=$(grep -oE '"scan_time"[[:space:]]*:[[:space:]]*"[^"]+"' "$CACHE_FILE" | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
        printf '{"scanning":false,"done":true,"count":%d,"scan_time":"%s"}\n' "$count" "$time"
    else
        echo '{"scanning":false,"done":false,"count":0,"scan_time":""}'
    fi
    exit 0
fi

# 防重复
if [ -f "$SCAN_LOCK" ]; then
    LOCK_PID=$(cat "$SCAN_LOCK" 2>/dev/null)
    if [ -n "$LOCK_PID" ] && kill -0 "$LOCK_PID" 2>/dev/null; then
        echo '{"ok":false,"error":"已在扫描中"}'
        exit 0
    fi
fi
echo $$ > "$SCAN_LOCK"
trap 'rm -f "$SCAN_LOCK"' EXIT

# 确保 aapt2 可执行
chmod +x "$AAPT2" 2>/dev/null

# 验证 aapt2
if [ ! -x "$AAPT2" ]; then
    log "❌ aapt2 不可用: $AAPT2"
    rm -f "$SCAN_LOCK"
    echo '{"ok":false,"error":"aapt2 不可用，请检查模块文件"}'
    exit 1
fi

mkdir -p "$ICON_DIR" 2>/dev/null
chmod 755 "$ICON_DIR" 2>/dev/null

log "===== 开始扫描 PID=$$ ====="
log "aapt2: $AAPT2"

# 提取 SSAID 流（一次性）
SSAID_STREAM_FILE="/data/local/tmp/webui_ssaid_stream_$$.txt"
strings "$SSAID_FILE" 2>/dev/null > "$SSAID_STREAM_FILE"

get_ssaid() {
    local pkg="$1"
    local id
    id=$(grep -A 1 "$pkg" "$SSAID_STREAM_FILE" 2>/dev/null | grep -oE '[0-9a-fA-F]{16}' | head -n 1)
    [ -z "$id" ] && id=$(grep "$pkg" "$SSAID_STREAM_FILE" 2>/dev/null | grep -oE '[0-9a-fA-F]{16}' | head -n 1)
    echo "$id"
}

is_valid_image() {
    local file="$1"
    [ ! -s "$file" ] && return 1
    local magic
    magic=$(od -An -tx1 -N4 "$file" 2>/dev/null | tr -d ' \n')
    case "$magic" in
        89504e47*) return 0 ;;
        52494646*) return 0 ;;
        *) return 1 ;;
    esac
}

# 用 aapt2 提取应用元信息（label + icon path）
# 输出: label|icon_res_path
get_app_meta() {
    local apk_path="$1"
    [ ! -f "$apk_path" ] && { echo "|"; return; }

    local badging
    badging=$("$AAPT2" dump badging "$apk_path" 2>/dev/null)
    [ -z "$badging" ] && { echo "|"; return; }

    # 提取中文 label（优先级：zh-CN > zh > 默认）
    local label=""
    label=$(echo "$badging" | grep -E "^application-label-zh-CN:" | head -1 | sed "s/^[^:]*:'//;s/'$//")
    [ -z "$label" ] && label=$(echo "$badging" | grep -E "^application-label-zh-rCN:" | head -1 | sed "s/^[^:]*:'//;s/'$//")
    [ -z "$label" ] && label=$(echo "$badging" | grep -E "^application-label-zh:" | head -1 | sed "s/^[^:]*:'//;s/'$//")
    [ -z "$label" ] && label=$(echo "$badging" | grep -E "^application-label:" | head -1 | sed "s/^[^:]*:'//;s/'$//")

    # 提取最高分辨率的图标路径
    local icon_path
    icon_path=$(echo "$badging" \
        | grep -oE "application-icon-[0-9]+:'[^']+'" \
        | sort -t- -k3 -n -r \
        | head -1 \
        | sed "s/^[^:]*:'//;s/'$//")

    echo "${label}|${icon_path}"
}

# 提取图标到 webroot/icons/<pkg>.png
extract_icon() {
    local pkg="$1"
    local apk_path="$2"
    local icon_res="$3"
    local cache_file="$ICON_DIR/${pkg}.png"

    # 已有有效缓存则跳过
    if is_valid_image "$cache_file"; then
        return 0
    fi

    [ -z "$icon_res" ] && return 1
    [ ! -f "$apk_path" ] && return 1

    unzip -p "$apk_path" "$icon_res" > "$cache_file" 2>/dev/null
    if is_valid_image "$cache_file"; then
        chmod 644 "$cache_file" 2>/dev/null
        return 0
    fi
    rm -f "$cache_file" 2>/dev/null
    return 1
}

# 处理单个应用
process_one_app() {
    local pkg="$1"
    [ -z "$pkg" ] && return

    local apk_path
    apk_path=$(pm path "$pkg" 2>/dev/null | head -1 | sed 's/^package://')
    [ -z "$apk_path" ] && return

    # SSAID
    local ssaid
    ssaid=$(get_ssaid "$pkg")
    [ -z "$ssaid" ] && ssaid="未分配"

    # 版本
    local ver
    ver=$(dumpsys package "$pkg" 2>/dev/null | grep versionName | head -1 | awk -F= '{print $2}' | tr -d ' \r')
    [ -z "$ver" ] && ver="未知"
    ver=$(echo "$ver" | sed 's/"/\\"/g')

    # 用 aapt2 拿 label + icon path
    local meta
    meta=$(get_app_meta "$apk_path")
    local label=$(echo "$meta" | cut -d'|' -f1)
    local icon_res=$(echo "$meta" | cut -d'|' -f2-)

    # 兜底：没拿到 label 就用包名
    [ -z "$label" ] && label="$pkg"
    label=$(echo "$label" | sed 's/\\/\\\\/g;s/"/\\"/g')

    # 提取图标
    local has_icon=false
    if extract_icon "$pkg" "$apk_path" "$icon_res"; then
        has_icon=true
    fi

    printf '{"pkg":"%s","label":"%s","ssaid":"%s","ver":"%s","has_icon":%s}\n' \
        "$pkg" "$label" "$ssaid" "$ver" "$has_icon"
}

# 1. 应用列表
log "获取应用列表"
PKG_LIST_FILE="/data/local/tmp/webui_pkglist_$$.txt"
pm list packages -3 2>/dev/null | sed 's/^package://' | sort > "$PKG_LIST_FILE"
TOTAL=$(wc -l < "$PKG_LIST_FILE")
log "共 $TOTAL 个第三方应用"

if [ "$TOTAL" -lt 1 ]; then
    rm -f "$PKG_LIST_FILE" "$SSAID_STREAM_FILE"
    echo '{"ok":false,"error":"未找到第三方应用"}'
    exit 1
fi

printf '{"scanning":true,"current":0,"total":%d}' "$TOTAL" > "$PROGRESS_FILE"

# 2. 并行处理（4 worker）
log "开始并行扫描"
RESULT_DIR="/data/local/tmp/webui_scan_results_$$"
mkdir -p "$RESULT_DIR"

WORKERS=4
i=0
while IFS= read -r pkg; do
    [ -z "$pkg" ] && continue
    worker_id=$((i % WORKERS))
    echo "$pkg" >> "$RESULT_DIR/queue.$worker_id"
    i=$((i + 1))
done < "$PKG_LIST_FILE"

for w in 0 1 2 3; do
    QFILE="$RESULT_DIR/queue.$w"
    OFILE="$RESULT_DIR/output.$w"
    [ ! -f "$QFILE" ] && continue
    (
        > "$OFILE"
        while IFS= read -r pkg; do
            [ -z "$pkg" ] && continue
            process_one_app "$pkg" >> "$OFILE"
            CURRENT_COUNT=$(cat "$RESULT_DIR/output."* 2>/dev/null | wc -l)
            printf '{"scanning":true,"current":%d,"total":%d}' "$CURRENT_COUNT" "$TOTAL" > "$PROGRESS_FILE"
        done < "$QFILE"
    ) &
done

wait
log "扫描完成"

# 3. 合并结果
NOW="$(date '+%Y-%m-%d %H:%M:%S')"
TMP_CACHE="/data/local/tmp/webui_apps_cache_$$.json"

printf '{"scan_time":"%s","total":%d,"apps":[' "$NOW" "$TOTAL" > "$TMP_CACHE"
FIRST=1
for w in 0 1 2 3; do
    OFILE="$RESULT_DIR/output.$w"
    [ ! -f "$OFILE" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        if [ "$FIRST" = "1" ]; then FIRST=0; else printf "," >> "$TMP_CACHE"; fi
        printf "%s" "$line" >> "$TMP_CACHE"
    done < "$OFILE"
done
printf ']}' >> "$TMP_CACHE"

mv "$TMP_CACHE" "$CACHE_FILE"
chmod 644 "$CACHE_FILE"
log "已写入 cache"

rm -rf "$RESULT_DIR" "$PKG_LIST_FILE" "$SSAID_STREAM_FILE" "$PROGRESS_FILE" 2>/dev/null

REAL_COUNT=$(grep -oE '"pkg":"[^"]+"' "$CACHE_FILE" | wc -l)
ICON_COUNT=$(ls "$ICON_DIR" 2>/dev/null | wc -l)
log "完成: 应用 $REAL_COUNT, 图标 $ICON_COUNT"
printf '{"ok":true,"msg":"扫描完成","count":%d,"icons":%d,"scan_time":"%s"}\n' "$REAL_COUNT" "$ICON_COUNT" "$NOW"
