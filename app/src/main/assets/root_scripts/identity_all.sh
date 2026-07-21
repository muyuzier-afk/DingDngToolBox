#!/system/bin/sh
# 一键新机：一次随机化 序列号 / Android ID / OAID / MAC
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 用法: sh identity_all.sh run
#
# 串联调用同目录各标识脚本的 random，汇总为一个 JSON 报告。
# 每项取子脚本 JSON 的 "ok" 判定 success / failed。

MOD_DIR="/data/adb/ddj_toolbox"
SCRIPTS="$MOD_DIR/scripts"
. "$MOD_DIR/scripts/lib_log.sh"
init_log "identity_all"
ACTION="$1"

# 判断子脚本输出 JSON 是否 ok:true
json_ok() {
    echo "$1" | grep -qE '"ok"[[:space:]]*:[[:space:]]*true'
}

# 跑一个子脚本，返回 success / failed
run_one() {
    local script="$1"; shift
    local out
    out=$(sh "$SCRIPTS/$script" "$@" 2>/dev/null)
    json_ok "$out" && echo "success" || echo "failed"
}

cmd_run() {
    local sn_r=$(run_one serial.sh random)
    local aid_r=$(run_one android_id.sh random)
    local oaid_r=$(run_one oaid.sh random_oaid)
    local mac_r=$(run_one mac.sh random)
    log_json "一键新机 serial=$sn_r android_id=$aid_r oaid=$oaid_r mac=$mac_r"
    printf '{"ok":true,"msg":"一键新机完成","serial":"%s","android_id":"%s","oaid":"%s","mac":"%s"}\n' \
        "$sn_r" "$aid_r" "$oaid_r" "$mac_r"
}

case "$ACTION" in
    run) cmd_run ;;
    *)
        echo '{"ok":false,"error":"unknown action - use: run"}'
        exit 1 ;;
esac
