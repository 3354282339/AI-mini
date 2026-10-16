# AI mini（Android / WebView 内核）

基于系统 `android.webkit.WebView` 的 AI mini 客户端。功能对齐 GeckoView 正式版（连接管理、下载、任务通知、桌面模式、界面设置、页面桥接等），浏览器内核保持 WebView，便于轻量安装与系统内核复用。

> 本工程由 GPT Mini（WebView 版 2.0.3）演进而来，现已作为**独立的新应用**：应用名 `AI mini`、包名 `app.aimini`、使用自己的发布签名，可与旧版 GPT Mini 并存安装。

当前版本：

- 应用名：`AI mini`
- versionName：`2.0.3`
- versionCode：`35`
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
app/build/outputs/apk/debug/AI mini-2.0.3-debug.apk
app/build/outputs/apk/release/AI mini-2.0.3-release.apk
```

## 签名

`keystore/ai-mini-release.jks`（别名 `aimini`）+ `keystore/ai-mini-signing.properties` 是 AI mini 自己的发布证书，两者都在 `.gitignore` 里，**不进仓库，必须单独备份**；丢了这个证书就无法再给已安装用户做覆盖升级。

签名指纹（SHA-256）：

```text
42A656FA6D22B8FE11C698EBEADB42CB85A6FAC4BC8FBBA5834EB8CAE5B718CF
```

## 相关文档

- [AI mini 应用介绍与使用帮助](AI%20mini-应用介绍与使用帮助.md)
- [AI mini 开发交接说明](AI-开发交接说明.md)
- [实况窗与通知适配说明](实况窗与通知适配说明.md)
