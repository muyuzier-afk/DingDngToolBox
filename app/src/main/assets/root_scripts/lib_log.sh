#!/system/bin/sh
# 统一日志库 - 日志以 JSONL 格式保存到 /data/adb/ddj_logs/<module>.json
# 每行一条 JSON：{"time":"...","tag":"...","msg":"..."}
# 作者: 呆呆 | QQ:891354018 | 群:774886621

WEBUI_LOG_DIR="/data/adb/ddj_logs"

# 初始化日志：init_log <module_name>
# 设置 LOG_FILE / LOG_TAG，并确保目录存在
init_log() {
    local name="$1"
    [ -z "$name" ] && name="webui"
    LOG_FILE="$WEBUI_LOG_DIR/${name}.json"
    LOG_TAG="$name"
    [ ! -d "$WEBUI_LOG_DIR" ] && mkdir -p "$WEBUI_LOG_DIR" 2>/dev/null
}

# 转义字符串供 JSON 使用（反斜杠、双引号、制表符、换行）
_log_escape() {
    printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g; s/	/\\t/g' | tr '\n' ' '
}

# 写 JSONL 日志：log_json <msg>
log_json() {
    [ -z "$LOG_FILE" ] && init_log "webui"
    local msg
    msg=$(_log_escape "$1")
    local now
    now=$(date '+%Y-%m-%d %H:%M:%S')
    printf '{"time":"%s","tag":"%s","msg":"%s"}\n' "$now" "$LOG_TAG" "$msg" >> "$LOG_FILE" 2>/dev/null
}

# 读取最近 N 条日志并格式化为文本供显示：read_log_text <n>
read_log_text() {
    local n="${1:-30}"
    [ -z "$LOG_FILE" ] && { echo "(日志未初始化)"; return; }
    [ ! -f "$LOG_FILE" ] && { echo "(无日志)"; return; }
    tail -n "$n" "$LOG_FILE" 2>/dev/null | while IFS= read -r line; do
        local t m
        t=$(echo "$line" | sed -n 's/.*"time":"\([^"]*\)".*/\1/p')
        m=$(echo "$line" | sed -n 's/.*"msg":"\([^"]*\)".*/\1/p')
        if [ -n "$t" ]; then
            printf '[%s] %s\n' "$t" "$m"
        else
            printf '%s\n' "$line"
        fi
    done
}

# 原始 JSONL 输出（供前端解析）：read_log_jsonl <n>
read_log_jsonl() {
    local n="${1:-100}"
    [ -z "$LOG_FILE" ] && { echo ""; return; }
    [ ! -f "$LOG_FILE" ] && { echo ""; return; }
    tail -n "$n" "$LOG_FILE" 2>/dev/null
}
