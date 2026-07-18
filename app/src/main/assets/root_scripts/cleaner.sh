#!/system/bin/sh
# 缓存清理脚本 - 无畏契约 + 和平精英
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 用法:
#   sh cleaner.sh                    深度清理（默认，输出 JSON）
#   sh cleaner.sh deep               深度清理（输出 JSON）
#   sh cleaner.sh light              轻度清理（输出 JSON）
#   sh cleaner.sh inotify            仅优化 inotify 参数（静默）
#   sh cleaner.sh pubgmhd_light      和平精英轻度清理（输出 JSON）
#   sh cleaner.sh pubgmhd_deep       和平精英深度清理（输出 JSON）

MOD_DIR="/data/adb/ddj_toolbox"
SETTINGS_FILE="$MOD_DIR/settings.conf"
ARG1="$1"

. "$MOD_DIR/scripts/lib_log.sh"
init_log "cleaner"

log() {
    log_json "$1"
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

PKG="com.tencent.tmgp.codev"
EXT_DIR="/storage/emulated/0/Android/data/$PKG"
INT_DIR="/data/user/0/$PKG"

PUBG_PKG="com.tencent.tmgp.pubgmhd"
PUBG_INT_DIR="/data/user/0/$PUBG_PKG"
PUBG_FILES_DIR="$PUBG_INT_DIR/files"
PUBG_EXT_DIR="/storage/emulated/0/Android/data/$PUBG_PKG"

is_pkg_installed() {
    local pkg="$1"
    pm path "$pkg" >/dev/null 2>&1 && return 0
    cmd package path "$pkg" >/dev/null 2>&1 && return 0
    [ -d "/data/data/$pkg" ] && return 0
    [ -d "/data/user/0/$pkg" ] && return 0
    [ -d "/storage/emulated/0/Android/data/$pkg" ] && return 0
    return 1
}

optimize_inotify() {
    echo 16384 > /proc/sys/fs/inotify/max_queued_events 2>/dev/null
    echo 128 > /proc/sys/fs/inotify/max_user_instances 2>/dev/null
    echo 8192 > /proc/sys/fs/inotify/max_user_watches 2>/dev/null
}

clean_light() {
    rm -rf /data/user/*/"$PKG/files/ano_tmp" 2>/dev/null
    rm -rf "$INT_DIR/cache" "$INT_DIR/code_cache" 2>/dev/null
    return 0
}

clean_deep() {
    optimize_inotify
    rm -rf /data/user/*/"$PKG/files/ano_tmp" 2>/dev/null

    if [ ! -d "$EXT_DIR" ] && [ ! -d "$INT_DIR" ]; then
        return 1
    fi

    [ -d "$EXT_DIR" ] && find "$EXT_DIR" -mindepth 1 -maxdepth 1 ! -name files -exec rm -r {} + 2>/dev/null

    [ -d "$EXT_DIR/files" ] && find "$EXT_DIR/files/" -mindepth 1 -maxdepth 1 \
        ! \( -name EstvShadowPlugin_shadow-app -o -name VulkanProgramBinaryCache -o -name UE4Game \) \
        -exec rm -r {} + 2>/dev/null

    if [ -d "$EXT_DIR/files/UE4Game/CodeV" ]; then
        find "$EXT_DIR/files/UE4Game/CodeV/" -maxdepth 1 -type f 2>/dev/null | while read f; do
            fn=$(basename "$f")
            case "$fn" in
                Manifest_UFSFiles_Android.txt|NotAllowedUnattendedBugReports|Version.cfg|ClearFlag_*|BagSkinCache_*.json|PlayerData.cfg|PCache.meta|Manifest.ini)
                    continue ;;
                *)
                    rm -f "$f" ;;
            esac
        done
    fi

    SAVED="$EXT_DIR/files/UE4Game/CodeV/CodeV/Saved"
    if [ -d "$SAVED" ]; then
        rm -rf "$SAVED/Gamelet/logs" "$SAVED/Gamelet/cookies" 2>/dev/null
        find "$SAVED/" -mindepth 1 -maxdepth 1 \
            ! \( -name ClearFlag_* -o -name ImageDownload -o -name Gamelet -o -name ShaderCache -o -name Paks -o -name MMKV -o -name Version.cfg -o -name PlayerData.cfg -o -name PCache.meta \) \
            -exec rm -r {} + 2>/dev/null
    fi

    rm -rf "$INT_DIR/cache" "$INT_DIR/code_cache" 2>/dev/null

    [ -d "$INT_DIR/files" ] && find "$INT_DIR/files/" -mindepth 1 -maxdepth 1 \
        ! \( -name ves -o -name live -o -name EstvShadowDir -o -name estv-valm-RELEASE \) \
        -exec rm -r {} + 2>/dev/null

    [ -d "$INT_DIR" ] && find "$INT_DIR/" -mindepth 1 -maxdepth 1 \
        ! \( -name app_tbs_64 -o -name files \) -exec rm -r {} + 2>/dev/null

    return 0
}

clean_pubgmhd_light() {
    if ! is_pkg_installed "$PUBG_PKG"; then
        return 1
    fi
    rm -rf "$PUBG_INT_DIR/cache" "$PUBG_INT_DIR/code_cache" 2>/dev/null
    [ -d "$PUBG_EXT_DIR/cache" ] && rm -rf "$PUBG_EXT_DIR/cache" 2>/dev/null
    return 0
}

clean_pubgmhd_deep() {
    if ! is_pkg_installed "$PUBG_PKG"; then
        return 1
    fi
    if [ -d "$PUBG_INT_DIR" ]; then
        find "$PUBG_INT_DIR/" -mindepth 1 -maxdepth 1 ! -name files -exec rm -r {} + 2>/dev/null
    fi
    if [ -d "$PUBG_FILES_DIR" ]; then
        find "$PUBG_FILES_DIR/" -mindepth 1 -maxdepth 1 ! -name ProgramBinaryCache -exec rm -r {} + 2>/dev/null
    fi
    if [ -d "$PUBG_EXT_DIR" ]; then
        find "$PUBG_EXT_DIR/" -mindepth 1 -maxdepth 1 ! -name files -exec rm -r {} + 2>/dev/null
        [ -d "$PUBG_EXT_DIR/files" ] && find "$PUBG_EXT_DIR/files/" -mindepth 1 -maxdepth 1 ! -name ProgramBinaryCache -exec rm -r {} + 2>/dev/null
    fi
    return 0
}

run_and_report() {
    local mode="$1"
    local label
    if [ "$mode" = "light" ]; then
        clean_light
        RET=$?
        label="轻度清理"
    else
        clean_deep
        RET=$?
        label="深度清理"
    fi

    if [ $RET -eq 0 ]; then
        NOW="$(date '+%Y-%m-%d %H:%M:%S')"
        write_setting "last_clean_time" "$NOW"
        log "$label 完成"
        printf '{"ok":true,"msg":"%s完成","mode":"%s","time":"%s"}\n' "$label" "$mode" "$NOW"
    else
        log "$label 失败：游戏未安装"
        echo '{"ok":false,"error":"无畏契约未安装"}'
    fi
}

run_pubgmhd_and_report() {
    local mode="$1"
    local label
    if [ "$mode" = "light" ]; then
        clean_pubgmhd_light
        RET=$?
        label="和平精英轻度清理"
    else
        clean_pubgmhd_deep
        RET=$?
        label="和平精英深度清理"
    fi

    if [ $RET -eq 0 ]; then
        NOW="$(date '+%Y-%m-%d %H:%M:%S')"
        log "$label 完成"
        printf '{"ok":true,"msg":"%s完成","mode":"%s","time":"%s"}\n' "$label" "$mode" "$NOW"
    else
        log "$label 失败：游戏未安装"
        echo '{"ok":false,"error":"和平精英未安装"}'
    fi
}

case "$ARG1" in
    inotify)
        optimize_inotify
        ;;
    light)
        run_and_report "light"
        ;;
    deep|"")
        run_and_report "deep"
        ;;
    pubgmhd_light)
        run_pubgmhd_and_report "light"
        ;;
    pubgmhd_deep)
        run_pubgmhd_and_report "deep"
        ;;
    *)
        echo '{"ok":false,"error":"unknown mode"}'
        exit 1
        ;;
esac

exit 0
