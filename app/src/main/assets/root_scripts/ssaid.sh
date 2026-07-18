#!/system/bin/sh
# SSAID 管理脚本（v2 - 同时支持 8 款游戏列表和全部应用列表）
# 作者: 呆呆 | QQ:891354018 | 群:774886621
# 用法:
#   sh ssaid.sh list              老的 6 款腾讯游戏（带详细图标）
#   sh ssaid.sh list_all          所有第三方应用（读 apps-cache.json）
#   sh ssaid.sh scan_status       查询扫描状态
#   sh ssaid.sh rescan            触发后台重新扫描
#   sh ssaid.sh reset <pkg>       重置某应用 SSAID
#   sh ssaid.sh get <pkg>         查询某应用 SSAID

MOD_DIR="/data/adb/ddj_toolbox"
SSAID_FILE="/data/system/users/0/settings_ssaid.xml"
APPS_CACHE="$MOD_DIR/apps-cache.json"
ICON_CACHE_DIR="$MOD_DIR/webroot/icons"
ACTION="$1"
PKG="$2"

# 8 款游戏专用图标路径预设
TARGET_APPS="com.tencent.ngr|王者荣耀世界|res/drawable-xhdpi-v4/icon.png|res/mipmap-xxxhdpi-v4/ic_launcher.png|res/mipmap-xxhdpi-v4/ic_launcher.png
com.tencent.tmgp.pubgmhd|和平精英|res/drawable-xxhdpi-v4/icon.png|res/drawable-xhdpi-v4/icon.png|res/drawable-hdpi-v4/icon.png
com.tencent.tmgp.sgame|王者荣耀|res/mipmap-xxxhdpi-v4/app_icon_round.png|res/mipmap-xxxhdpi-v4/app_icon.png|res/drawable-xxxhdpi-v4/icon.png
com.tencent.tmgp.codev|无畏契约手游|res/drawable-xxxhdpi-v4/icon.png|res/mipmap-xxxhdpi-v4/ic_launcher.png|res/drawable-xxhdpi-v4/icon.png
com.tencent.tmgp.cf|穿越火线枪战王者|res/drawable-xxxhdpi-v4/app_icon.png|assets/AppIcon/UnCompress_CF_Appicon_192.png|res/drawable-xxhdpi-v4/app_icon.png
com.tencent.tmgp.cod|使命召唤手游|res/mipmap-xxxhdpi-v4/ic_launcher.png|res/drawable-xxxhdpi-v4/icon.png|res/mipmap-xxhdpi-v4/ic_launcher.png
com.tencent.tmgp.dfm|三角洲行动|res/mipmap-xxxhdpi-v4/ic_launcher.png|res/drawable-xxxhdpi-v4/icon.png|res/mipmap-xxhdpi-v4/ic_launcher.png
com.tencent.mf.uam|暗区突围|res/mipmap-xxxhdpi-v4/ic_launcher.png|res/drawable-xxxhdpi-v4/icon.png|res/mipmap-xxhdpi-v4/ic_launcher.png"

gen_random_id() {
    if [ -r /dev/urandom ]; then
        cat /dev/urandom | tr -dc 'a-f0-9' | head -c 16 2>/dev/null
    else
        echo "$(date +%s)$$RANDOM" | md5sum | head -c 16
    fi
}

get_ssaid() {
    local pkg="$1"
    local id
    local stream
    stream=$(strings "$SSAID_FILE" 2>/dev/null)
    [ -z "$stream" ] && return
    id=$(echo "$stream" | grep -A 1 "$pkg" | grep -oE '[0-9a-fA-F]{16}' | head -n 1)
    [ -n "$id" ] && { echo "$id"; return; }
    id=$(echo "$stream" | grep "$pkg" | grep -oE '[0-9a-fA-F]{16}' | head -n 1)
    echo "$id"
}

is_installed() { pm path "$1" >/dev/null 2>&1; }
get_apk_path() { pm path "$1" 2>/dev/null | head -1 | sed 's/^package://'; }

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

# 6 款游戏的图标 base64（保持不变）
get_icon_base64_inline() {
    local pkg="$1"
    local p1="$2" p2="$3" p3="$4"

    mkdir -p "$ICON_CACHE_DIR" 2>/dev/null
    local cache_file="$ICON_CACHE_DIR/${pkg}.png"

    if is_valid_image "$cache_file"; then
        if command -v base64 >/dev/null 2>&1; then
            local b64
            b64=$(base64 -w 0 "$cache_file" 2>/dev/null || base64 "$cache_file" 2>/dev/null | tr -d '\n')
            [ -n "$b64" ] && { echo "data:image/png;base64,$b64"; return; }
        fi
    fi

    local apk_path=$(get_apk_path "$pkg")
    [ -z "$apk_path" ] && return
    [ ! -f "$apk_path" ] && return

    for try_path in "$p1" "$p2" "$p3"; do
        [ -z "$try_path" ] && continue
        unzip -p "$apk_path" "$try_path" > "$cache_file" 2>/dev/null
        if is_valid_image "$cache_file"; then
            chmod 644 "$cache_file" 2>/dev/null
            if command -v base64 >/dev/null 2>&1; then
                local b64
                b64=$(base64 -w 0 "$cache_file" 2>/dev/null || base64 "$cache_file" 2>/dev/null | tr -d '\n')
                [ -n "$b64" ] && { echo "data:image/png;base64,$b64"; return; }
            fi
        fi
    done
    rm -f "$cache_file" 2>/dev/null
}

case "$ACTION" in
    # ============ 老接口：6 款腾讯游戏 ============
    list)
        TMP_RESULT="/data/local/tmp/ssaid_list_$$.txt"
        > "$TMP_RESULT"

        echo "$TARGET_APPS" | while IFS='|' read -r PKG_LINE LABEL ICON1 ICON2 ICON3; do
            [ -z "$PKG_LINE" ] && continue
            [ -z "$LABEL" ] && continue
            is_installed "$PKG_LINE" || continue

            SSAID_VAL=$(get_ssaid "$PKG_LINE")
            [ -z "$SSAID_VAL" ] && SSAID_VAL="未分配"
            VER=$(dumpsys package "$PKG_LINE" 2>/dev/null | grep versionName | head -1 | awk -F= '{print $2}' | tr -d ' \r')
            ICON=$(get_icon_base64_inline "$PKG_LINE" "$ICON1" "$ICON2" "$ICON3")
            LABEL_ESC=$(echo "$LABEL" | sed 's/\\/\\\\/g;s/"/\\"/g')
            VER_ESC=$(echo "$VER" | sed 's/"/\\"/g')

            printf '{"pkg":"%s","label":"%s","ssaid":"%s","ver":"%s","icon":"%s"}\n' \
                "$PKG_LINE" "$LABEL_ESC" "$SSAID_VAL" "$VER_ESC" "$ICON" >> "$TMP_RESULT"
        done

        printf '{"apps":['
        FIRST=1
        while IFS= read -r LINE; do
            [ -z "$LINE" ] && continue
            if [ "$FIRST" = "1" ]; then FIRST=0; else printf ','; fi
            printf '%s' "$LINE"
        done < "$TMP_RESULT"
        printf ']}'
        rm -f "$TMP_RESULT"
        ;;

    # ============ 新接口：全部应用（读 cache）============
    list_all)
        if [ ! -f "$APPS_CACHE" ]; then
            echo '{"ok":false,"need_scan":true,"error":"应用列表尚未生成"}'
            exit 0
        fi
        # 直接 cat 缓存文件（前端自己解析）
        # 在最外层 wrap 一下，加 ok 字段
        local data=$(cat "$APPS_CACHE")
        # 把 {"scan_time":...} 改成 {"ok":true,"scan_time":...}
        echo "$data" | sed 's/^{/{"ok":true,/'
        ;;

    # ============ 扫描状态 ============
    scan_status)
        sh "$MOD_DIR/scripts/scan_apps.sh" status
        ;;

    # ============ 触发重新扫描（异步）============
    rescan)
        # 后台跑
        nohup sh "$MOD_DIR/scripts/scan_apps.sh" >/dev/null 2>&1 &
        echo '{"ok":true,"msg":"扫描已在后台启动"}'
        ;;

    # ============ 查询单个 SSAID ============
    get)
        [ -z "$PKG" ] && { echo '{"ok":false,"error":"missing pkg"}'; exit 1; }
        SSAID_VAL=$(get_ssaid "$PKG")
        printf '{"ok":true,"pkg":"%s","ssaid":"%s"}\n' "$PKG" "${SSAID_VAL:-未分配}"
        ;;

    # ============ 重置 SSAID ============
    reset)
        [ -z "$PKG" ] && { echo '{"ok":false,"error":"missing pkg"}'; exit 1; }
        [ ! -f "$SSAID_FILE" ] && { echo '{"ok":false,"error":"ssaid file not found"}'; exit 1; }

        OLD_ID=$(get_ssaid "$PKG")
        NEW_ID=$(gen_random_id)

        if [ -z "$OLD_ID" ]; then
            MAX_ID=$(strings "$SSAID_FILE" 2>/dev/null | grep -oE '_id="[0-9]+"' | grep -oE '[0-9]+' | sort -n | tail -1)
            NEXT_ID=$(( ${MAX_ID:-0} + 1 ))
            sed -i "s|</settings>|<setting id=\"$NEXT_ID\" name=\"$PKG\" value=\"$NEW_ID\" package=\"android\" defaultValue=\"$NEW_ID\" defaultSysSet=\"true\" />\n</settings>|" "$SSAID_FILE"
            RESULT="inserted"
        else
            sed -i "s/$OLD_ID/$NEW_ID/g" "$SSAID_FILE"
            RESULT="updated"
        fi

        CHECK=$(get_ssaid "$PKG")
        if [ "$CHECK" = "$NEW_ID" ]; then
            printf '{"ok":true,"pkg":"%s","old":"%s","new":"%s","action":"%s"}\n' "$PKG" "${OLD_ID:-none}" "$NEW_ID" "$RESULT"
        else
            printf '{"ok":false,"pkg":"%s","error":"verify failed"}\n' "$PKG"
        fi
        ;;

    *)
        echo '{"ok":false,"error":"unknown action"}'
        ;;
esac
