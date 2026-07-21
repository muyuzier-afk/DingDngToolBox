#!/system/bin/sh
# 触控采样率提升脚本
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 用法:
#   sh touch_rate.sh set <profile>   设置触控采样率档位
#   sh touch_rate.sh status          查询当前档位
#   sh touch_rate.sh stop            停止循环守护并恢复默认

MOD_DIR="/data/adb/ddj_toolbox"
PROFILE_FILE="$MOD_DIR/touch_profile"
DAEMON_PIDFILE="/data/local/tmp/webui_touch_daemon.pid"
ACTION="$1"
PROFILE="$2"

. "$MOD_DIR/scripts/lib_log.sh"
init_log "touch"

log() {
    log_json "$1"
}

cmd_for() {
    case "$1" in
        125)  echo "touchHidlTest -c wo 0 26 0" ;;
        240)  echo "touchHidlTest -c wo 0 26 1" ;;
        360)  echo "touchHidlTest -c wo 0 26 12c" ;;
        600)  echo "touchHidlTest -c wo 0 26 258" ;;
        241)  echo "touchHidlTest -c wo 0 182 240" ;;
        361)  echo "touchHidlTest -c wo 0 26 c" ;;
        362)  echo "touchHidlTest -c wo 0 182 360" ;;
        feel_smooth) echo "touchHidlTest -c wo 0 24 3 ; touchHidlTest -c wo 0 25 2" ;;
        feel_game)   echo "touchHidlTest -c wo 0 24 5 ; touchHidlTest -c wo 0 25 4" ;;
        feel_max)    echo "touchHidlTest -c wo 0 24 5 ; touchHidlTest -c wo 0 25 5" ;;
        *)    echo "" ;;
    esac
}

stop_daemon() {
    if [ -f "$DAEMON_PIDFILE" ]; then
        local pid=$(cat "$DAEMON_PIDFILE")
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            kill -9 "$pid" 2>/dev/null
        fi
        rm -f "$DAEMON_PIDFILE"
    fi
}

start_daemon() {
    stop_daemon
    local script="$MOD_DIR/scripts/touch_daemon.sh"
    if [ -f "$script" ]; then
        nohup sh "$script" >/dev/null 2>&1 &
        echo $! > "$DAEMON_PIDFILE"
        log "循环守护已启动 (PID: $!)"
    fi
}

case "$ACTION" in
    set)
        [ -z "$PROFILE" ] && { echo '{"ok":false,"error":"missing profile"}'; exit 1; }
        if [ "$PROFILE" = "default" ]; then
            stop_daemon
            echo "default" > "$PROFILE_FILE"
            log "已恢复默认，停止循环守护"
            printf '{"ok":true,"profile":"default","msg":"已恢复默认触控采样率"}\n'
        else
            CMD=$(cmd_for "$PROFILE")
            if [ -z "$CMD" ]; then
                printf '{"ok":false,"error":"未知档位: %s"}\n' "$PROFILE"
                exit 1
            fi
            sh -c "$CMD" 2>/dev/null
            log "立即执行: $CMD"
            echo "$PROFILE" > "$PROFILE_FILE"
            start_daemon
            printf '{"ok":true,"profile":"%s","msg":"触控采样率已设置为 %s"}\n' "$PROFILE" "$PROFILE"
        fi
        ;;

    status)
        local cur="default"
        [ -f "$PROFILE_FILE" ] && cur=$(cat "$PROFILE_FILE" 2>/dev/null)
        [ -z "$cur" ] && cur="default"
        local daemon_running="false"
        if [ -f "$DAEMON_PIDFILE" ]; then
            local pid=$(cat "$DAEMON_PIDFILE")
            if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
                daemon_running="true"
            fi
        fi
        printf '{"ok":true,"profile":"%s","daemon":%s}\n' "$cur" "$daemon_running"
        ;;

    stop)
        stop_daemon
        echo "default" > "$PROFILE_FILE"
        log "手动停止守护并恢复默认"
        printf '{"ok":true,"msg":"已停止触控守护"}\n'
        ;;

    *)
        echo '{"ok":false,"error":"unknown action - use: set|status|stop"}'
        exit 1
        ;;
esac
