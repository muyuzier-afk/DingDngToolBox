# 自动签名与 GitHub Release

本项目为开源发行场景，**签名密钥直接放在仓库** `keystore/` 目录，无需配置 GitHub Secrets。

## 仓库内文件

| 路径 | 说明 |
|------|------|
| `keystore/dingdongji-release.jks` | release 密钥库 |
| `keystore/signing.properties` | 密码与 alias |

`app/build.gradle.kts` 默认读取上述文件；本地与 CI 行为一致。

## 发版

```bash
git tag v0.0.1
git push origin v0.0.1
```

或在 Actions 页手动运行 **Release**，输入 tag。

规则：

- `versionName` = 去掉 `v` 后的字符串（`v0.0.1` → `0.0.1`）
- `versionCode` = `主*10000 + 次*100 + 补丁`（`0.0.1` → `1`）

## 本地构建已签名 APK

```bash
gradle :app:assembleRelease
# 产物：app/build/outputs/apk/release/
```

## 工作流分工

- `build.yml`：push / PR 打 debug（日常 CI）
- `release.yml`：tag / 手动触发，打 signed release 并上传到 GitHub Releases

## 注意

密钥在公开仓库中可见，任何人都能用同一证书签名。对社区开源 App 一般可接受；若以后要上架商店或更高安全要求，再换成私有 Secrets 即可。
