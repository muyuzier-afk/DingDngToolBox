#!/system/bin/sh
# 触控采样率循环守护脚本
# 每隔10秒重新执行一次触控命令，防止系统重置
# 作者: 呆呆 | QQ:891354018 | 群:774886621

MOD_DIR="/data/adb/ddj_toolbox"
PROFILE_FILE="$MOD_DIR/touch_profile"
INTERVAL=10

while true; do
    if [ -f "$PROFILE_FILE" ]; then
        profile=$(cat "$PROFILE_FILE" 2>/dev/null)
        case "$profile" in
            125) touchHidlTest -c wo 0 26 0 2>/dev/null ;;
            240) touchHidlTest -c wo 0 26 1 2>/dev/null ;;
            360) touchHidlTest -c wo 0 26 12c 2>/dev/null ;;
            600) touchHidlTest -c wo 0 26 258 2>/dev/null ;;
            241) touchHidlTest -c wo 0 182 240 2>/dev/null ;;
            361) touchHidlTest -c wo 0 26 c 2>/dev/null ;;
            362) touchHidlTest -c wo 0 182 360 2>/dev/null ;;
            feel_smooth) touchHidlTest -c wo 0 24 3 2>/dev/null; touchHidlTest -c wo 0 25 2 2>/dev/null ;;
            feel_game)   touchHidlTest -c wo 0 24 5 2>/dev/null; touchHidlTest -c wo 0 25 4 2>/dev/null ;;
            feel_max)    touchHidlTest -c wo 0 24 5 2>/dev/null; touchHidlTest -c wo 0 25 5 2>/dev/null ;;
        esac
    fi
    sleep $INTERVAL
done
