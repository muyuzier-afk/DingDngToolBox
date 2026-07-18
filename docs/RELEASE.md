# 自动签名与 GitHub Release

推送符合 `v*` 的 tag（例如 `v0.0.1`）后，工作流 `.github/workflows/release.yml` 会：

1. 解码 GitHub Secrets 中的签名密钥
2. 构建 **已签名** release APK
3. 用 `apksigner` 校验签名
4. 创建 GitHub Release 并挂上 APK

## 1. 准备密钥（一次性）

若还没有 release 密钥库：

```bash
keytool -genkeypair -v \
  -keystore dingdongji-release.jks \
  -storetype JKS \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias dingdongji \
  -storepass '<STORE_PASSWORD>' \
  -keypass '<KEY_PASSWORD>' \
  -dname "CN=DingDongJi Toolbox, OU=OpenSource, O=DingDongJi, C=CN"
```

导出 Base64（用于 Secret `KEYSTORE_BASE64`）：

```bash
base64 -w 0 dingdongji-release.jks
```

**务必离线备份** `.jks` 与密码。丢失后无法对同一签名证书升级安装。

## 2. 配置 GitHub Secrets

仓库 → Settings → Secrets and variables → Actions → New repository secret：

| Secret 名称 | 内容 |
|-------------|------|
| `KEYSTORE_BASE64` | `dingdongji-release.jks` 的 base64（一行） |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 别名，例如 `dingdongji` |
| `KEY_PASSWORD` | key 密码（可与 store 相同） |

`GITHUB_TOKEN` 由 Actions 自动注入，无需手建。

## 3. 发布版本

```bash
# 建议语义化版本：v主.次.补丁
git tag v0.0.1
git push origin v0.0.1
```

或在 Actions 页手动运行 **Release**，输入 tag（如 `v0.0.1`）。

版本规则：

- `versionName` = 去掉 `v` 后的字符串（`v0.0.1` → `0.0.1`）
- `versionCode` = `主*10000 + 次*100 + 补丁`（`0.0.1` → `1`）

## 4. 本地签名构建

```bash
cp keystore.properties.example keystore.properties
# 编辑 storeFile / 密码 / alias

./gradlew :app:assembleRelease
# 或使用系统 gradle：
gradle :app:assembleRelease
```

产物：`app/build/outputs/apk/release/`

## 5. 与日常 CI 的关系

- `build.yml`：push / PR 打 debug（及可选 unsigned release）
- `release.yml`：仅 tag / 手动触发，打 **signed** release 并上传到 Releases
