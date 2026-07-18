# 叮咚鸡工具箱 (DingDongJi Toolbox)

> 几乎全能的安卓开源玩机工具箱

当前版本：**Alpha 0.0.1-Preview**（设备信息 + 系统监控 + ROOT 脚本工具箱）。

## 功能

- **设备信息**：硬件 / 系统 / 屏幕 / 电池一览
- **系统监控**：CPU / 内存 / 电池 实时采样（每 1.5s）
- **Root / Shizuku 状态检测**：首页展示双引擎可用性
- **ROOT 工具箱**（需 Root）：内置 shell 脚本部署至 `/data/adb/ddj_toolbox`
  - 设备标识：Android ID / SSAID / OAID / 序列号
  - 机型伪装与预设
  - 防标记 / Persist 隐藏 / 应用隐藏冻结
  - 游戏缓存清理与自动监听
  - 触控采样率
  - 模块设置

## 技术栈

- Kotlin 2.0 + Jetpack Compose（Material 3）
- Min SDK 31（Android 12）/ Target SDK 34
- AGP 8.5 + Gradle 8.9
- 包名：`com.toolbox.ddj`
- Shizuku API 13.1.5（provider 已在 Manifest 中声明）

## 目录结构

```
.
├── app/                      # 应用模块
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/toolbox/ddj/
│       │   ├── DingDongJiApp.kt
│       │   ├── MainActivity.kt
│       │   ├── data/         # 数据模型 + Repository + Root 脚本目录
│       │   ├── ui/           # Compose 主题 / 组件 / 屏幕 / 导航
│       │   └── util/         # Root / Shizuku 工具类
│       ├── assets/root_scripts/  # 内置 ROOT shell 脚本
│       └── res/
├── gradle/libs.versions.toml # 版本目录
├── .github/workflows/build.yml  # CI 构建工作流
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 构建

> 本仓库**仅通过 GitHub Actions 构建**，未提交 Gradle Wrapper 二进制（`gradle-wrapper.jar`）。
> 如需本地构建，请自行安装 Gradle 8.9 或运行 `gradle wrapper` 生成 wrapper。

### 通过 GitHub Actions

推送到 `main`/`master` 或打 `v*` 标签即可自动触发，构建产物作为 Artifact 上传：

- `dingdongji-toolbox-debug`：Debug APK（可直接安装）
- `dingdongji-toolbox-release-unsigned`：未签名 Release APK（如需正式发布请自行配置签名）

工作流文件：[`.github/workflows/build.yml`](.github/workflows/build.yml)

### 本地构建（可选）

```bash
# 需要 JDK 17 与 Android SDK (platforms;android-34, build-tools;34.0.0)
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## ROOT 脚本说明

- 资源目录：`app/src/main/assets/root_scripts/`
- 运行时部署：`/data/adb/ddj_toolbox/scripts/`
- 日志目录：`/data/adb/ddj_logs/`
- 首次进入「ROOT 工具」页会自动部署；版本号见 `VERSION` 文件
- 脚本以 JSON 为主输出，危险操作（重置/重启等）需二次确认

## 后续路线

- 应用管理（提取 APK / 卸载 / 权限查看）
- 文件管理
- 本地 ADB / Shell 工具（Shizuku 驱动）
- Root：hosts 编辑、SELinux 状态、init.d 脚本
- 机型预设云同步 / 更完善的 SSAID 图标列表 UI
