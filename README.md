# AI mini（Android / WebView 内核）

基于系统 `android.webkit.WebView` 的 AI mini 客户端。功能对齐 GeckoView 正式版（连接管理、下载、任务通知、桌面模式、界面设置、页面桥接等），浏览器内核保持 WebView，便于轻量安装与系统内核复用。

> 本工程由 GPT Mini（WebView 版 2.0.3）演进而来，现已作为**独立的新应用**：应用名 `AI mini`、包名 `app.aimini`、使用自己的发布签名，可与旧版 GPT Mini 并存安装。

当前版本：

- 应用名：`AI mini`
- versionName：`1.0.1`
- versionCode：`2`
- 仓库：<https://github.com/3354282339/AI-mini>
- 下载安装包：<https://github.com/3354282339/AI-mini/releases/latest>
- applicationId：`app.aimini`（与旧版 `app.gptmini.webview`、Gecko 版 `app.gptmini` 互不冲突，可同时安装）
- 最低系统：Android 8.0（API 26）
- 浏览器内核：系统 WebView

## 构建

Windows（本机 JDK / Android SDK / Gradle 已配好，详见 `..\android-env.md`）：

```powershell
cd D:\aicoding\AImini\GPT-Mini-Android
.\gradlew.bat assembleDebug      # 调试包
.\gradlew.bat assembleRelease    # 正式包（需要签名配置，见下）
```

其他环境：

```bash
chmod +x scripts/build-debug.sh
./scripts/build-debug.sh
```

APK 输出：

```text
app/build/outputs/apk/debug/AI mini-1.0.1-debug.apk
app/build/outputs/apk/release/AI mini-1.0.1-release.apk
```

## 签名

`keystore/ai-mini-release.jks`（别名 `aimini`）+ `keystore/ai-mini-signing.properties` 是 AI mini 自己的发布证书，两者都在 `.gitignore` 里，**不进仓库，必须单独备份**；丢了这个证书就无法再给已安装用户做覆盖升级。

签名指纹（SHA-256）：

```text
42A656FA6D22B8FE11C698EBEADB42CB85A6FAC4BC8FBBA5834EB8CAE5B718CF
```

## 多设备 / 多连接

AI mini 按「**一个网址 = 一台设备**」管理连接，每台设备带一个**类型**，为后续接入不同产品（zcode、dsh …）留好位置：

| 类型 | 标识 | 说明 |
| --- | --- | --- |
| 本地 WebUI | `miniapp` | 当前这套 Codex / GPT Mini WebUI，支持任务状态通知 |
| zcode | `zcode` | 另一个 WebUI，按网址打开界面 |
| dsh | `dsh` | 同上 |
| 其它 | `other` | 任意 HTTP(S) 网页 |

- 首页「**我的设备**」列出已保存设备（最多 4 台，更多可点「查看全部」），点击即切换，当前设备高亮显示。
- 手动输入网址时可用「**设备类型**」下拉指定类型；类型也会按 URL 关键字自动推断（含 `zcode` → zcode，含 `dsh`/`deepseek` → dsh，含 `codex`/`mini` → 本地 WebUI），可随时在设备管理处改。
- 悬浮菜单第一项是「**切换设备**」，副标题显示 `类型 · 设备名`；点开就是设备列表。
- 「管理设备」里可重命名、**改类型**、删除；改完立即生效。
- 通知行为按类型分支：只有 `miniapp` 走 `/codex/status` 任务监控；zcode / dsh / 其它类型保留常驻通知但不发状态请求。

## 相关文档

- [AI mini 应用介绍与使用帮助](AI%20mini-应用介绍与使用帮助.md)
- [AI mini 开发交接说明](AI-开发交接说明.md)
- [实况窗与通知适配说明](实况窗与通知适配说明.md)
