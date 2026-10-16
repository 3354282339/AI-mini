# AI mini Android 开发交接说明

本文档用于把当前项目交给另一台电脑或另一个 AI 继续开发。它记录当前项目状态、已实现功能、关键文件、构建方式、签名信息和后续开发注意事项。

## 1. 项目概况

项目名称：AI mini

项目类型：Android 原生 WebView App

主要语言：Java

构建系统：Gradle / Android Gradle Plugin

当前版本：

- applicationId：`app.aimini`
- versionCode：`33`
- versionName：`2.0.1`
- App 显示名称：`AI mini`

当前定位：

这是一个为 Codex Mini / AI mini WebUI 优化的 Android 手机 App。核心仍是加载 WebUI，但针对 Android 手机上浏览器/WebView 使用中的键盘、下载、通知、连接切换、首页入口等问题做了原生层优化。

## 2. 目录结构

重要目录和文件：

- `app/src/main/java/com/coimgrain/codexminiapp/MainActivity.java`
  - App 主逻辑。
  - 包含首页、WebView、悬浮窗、下载界面、通知、局域网探测、下载记录持久化等功能。

- `app/src/main/java/com/coimgrain/codexminiapp/ScanActivity.java`
  - 扫码连接界面。

- `app/src/main/res/values/strings.xml`
  - App 名称、按钮文案、通知文案、下载文案等。

- `app/src/main/res/drawable/`
  - 图标资源。
  - 当前 App 图标使用 ChatGPT 风格图标资源。

- `app/src/main/res/xml/network_security_config.xml`
  - 网络配置。
  - 已允许 HTTP 明文流量，便于访问局域网或公网 HTTP WebUI。

- `GPTMini-应用介绍与使用帮助.md`
  - 面向用户的 App 介绍、优化说明和使用帮助。

- `AI-开发交接说明.md`
  - 当前文档，面向继续开发的 AI 或开发者。

- `scripts/build-debug.sh`
  - debug 构建脚本。

- `scripts/build-release.sh`
  - release 构建脚本。

- `keystore/ai-mini-release.jks`
  - release 签名证书。
  - 继续发布 `v1.1`、`v1.2` 等版本时必须继续使用这个证书。

- `keystore/ai-mini-signing.properties`
  - release 签名配置。
  - 包含证书路径、别名和密码。

## 3. 当前已实现功能

### 3.1 WebView 基础功能

- 加载 Codex Mini / AI mini WebUI。
- 支持 HTTP 和 HTTPS。
- 支持文件选择。
- 支持页面权限请求。
- 支持 WebUI 下载触发。
- 支持页面内通知状态回传到原生通知。

### 3.2 首页连接页

首页已经重新设计，用户首次打开时需要输入 WebUI 地址或扫码连接。

首页入口包括：

- 扫码连接。
- 已保存连接。
- 继续上次连接。
- 手动输入地址。
- 连接提示。

连接成功后会保存上次地址。后续打开 App 会默认继续上次连接。

### 3.3 已保存连接

已保存连接用于管理多个 WebUI 地址。

行为：

- 连接成功后自动保存地址。
- 相同地址不会重复保存。
- 重新连接已有地址时，会把该地址移动到列表最上方。
- 用户可以给地址设置备注名称。
- 用户可以删除保存记录。
- 点击保存记录可以直接连接。

### 3.4 键盘适配优化

已处理 Android WebView 中常见的输入框和键盘问题：

- 首次进入后点击输入框无法弹出键盘的问题。
- 点击键盘收起按钮后，输入框和页面位置不回落的问题。
- 复制消息等非输入操作误触发键盘的问题。

目标是让输入框行为更接近原生 App。

### 3.5 悬浮窗

App 使用侧边悬浮窗作为功能入口，避免顶部工具栏占用 WebUI 显示空间。

悬浮窗特性：

- 默认在屏幕侧边。
- 支持拖动。
- 支持侧边吸附。
- 支持半透明。
- 可调整大小。
- 主题支持深色、浅色、跟随系统。

悬浮窗面板按钮名称以实际 UI 为准：

- 下载。
- 回到连接页。
- 刷新当前页面。
- 悬浮窗设置。
- 通知设置。

### 3.6 下载管理

针对原 WebUI 点击下载后不知道文件在哪的问题，新增了 App 内下载界面。

下载界面能力：

- 显示下载记录。
- 显示下载状态。
- 显示下载时间。
- 最新下载显示在最上面。
- 长文件名最多两行显示，超出省略。
- 支持打开文件。
- 支持分享文件。
- 支持删除文件。
- 支持管理模式、全选、取消全选、批量删除。
- 支持深色、浅色主题，跟随悬浮窗主题设置。

重要行为：

- 删除按钮是真实删除手机本地文件，不只是隐藏列表记录。
- DownloadManager 下载使用 `DownloadManager.remove(id)` 删除。
- App 自己保存的文件使用 `ContentResolver.delete(uri)` 或 `File.delete()` 删除。
- 删除成功后才从下载列表移除。

### 3.7 下载记录持久化

下载记录已做成类似浏览器下载记录的保存方式。

当前保存内容包括：

- DownloadManager id。
- 是否为 App 手动保存文件。
- 文件名。
- mimeType。
- manualUri。
- localUri。
- 下载状态。
- 已下载大小。
- 总大小。
- 下载时间。

正常覆盖安装或升级 App 后，下载界面会恢复之前的下载记录。

注意：

- 如果用户手动卸载 App 再安装，Android 可能清除 App 本地配置，下载记录可能丢失。
- 文件本身通常仍在系统下载目录中。
- 如果用户在文件管理器里手动删除文件，App 下载记录可能仍显示，但打开/分享会失败，需要用户在 App 下载界面删除记录。

### 3.8 通知功能

通知模式有两种：

- 任务结束通知。
- 常驻通知。

任务结束通知：

- 只有任务完成、失败或异常时发送通知。

常驻通知：

- App 连接后常驻显示任务通知。
- 没有任务时显示空闲状态。
- 任务运行、完成、失败/异常时更新通知。

多任务通知：

- 每个会话任务使用独立通知。
- 同一个会话的运行中、完成、失败/异常会覆盖更新同一个通知，不重复新建多个通知。

### 3.9 局域网线路辅助识别

App 不会直接把 WebView 地址栏改成局域网地址。

当前策略：

- WebView 页面地址仍保持用户输入的公网 URL。
- 原生层探测局域网可用性。
- 探测可用后，辅助 WebUI 使用本地线路。
- 尽量让 WebUI 自己的右上角状态显示真实线路状态。

这不是系统浏览器的完全原生自动切换，而是 App 针对该 WebUI 做的辅助识别和注入。

### 3.10 纯文本/错误页适配

对一些纯文本、JSON、错误信息页面做了移动端显示适配。

目标：

- 避免超长文本撑出屏幕。
- 避免横向超宽显示。
- 让错误信息在手机屏幕上可读。

## 4. 当前 release 包状态

当前正式包：

- 输出位置：`outputs/GPTMini-1.0-release.apk`
- 原始 Gradle 输出：`app/build/outputs/apk/release/AI mini-1.0-release.apk`

已验证：

- release v1/v2 签名通过。
- zipalign 通过。
- 包名：`app.gptmini`（历史记录，旧版标识；当前 AI mini 使用 `app.aimini`）
- 版本：`1.0`

如果用户之前安装的是 debug 包，可能因为签名不同不能直接覆盖安装，需要先卸载 debug 版。

之后只要一直使用当前 release 证书打包，就可以正常覆盖升级。

## 5. 构建说明

当前项目没有提交 Gradle Wrapper，而是使用本机 `.tools` 目录里的 Gradle、JDK 和 Android SDK。

本源码压缩包通常不包含 `.tools`，因为该目录很大且属于本机工具依赖。迁移到新电脑后可选两种方式：

### 方式一：使用 Android Studio

1. 用 Android Studio 打开项目根目录。
2. 安装 Android SDK 35。
3. 等待 Gradle 同步。
4. 构建 debug 或 release。

### 方式二：使用命令行

需要准备：

- JDK。
- Android SDK。
- Gradle 8.x。
- Android Gradle Plugin 可正常解析依赖。

然后根据新电脑环境调整：

- `local.properties`
- `scripts/build-debug.sh`
- `scripts/build-release.sh`

当前脚本默认使用：

- `.tools/jdk/Contents/Home`
- `.tools/android-sdk`
- `.tools/gradle/gradle-8.10.2/bin/gradle`

如果新电脑没有 `.tools`，脚本需要改成系统自己的 `JAVA_HOME`、`ANDROID_HOME` 和 `gradle`。

## 6. 签名说明

release 签名文件：

- `keystore/ai-mini-release.jks`
- `keystore/ai-mini-signing.properties`

这两个文件对后续升级非常重要。

注意：

- 不要丢失 `gpt-mini-release.jks`。
- 不要随便换签名证书。
- 如果换证书，同包名 App 将无法覆盖升级。
- 不要把 keystore 和密码公开发布到公开仓库。

当前 `.gitignore` 已忽略 keystore 文件，但源码压缩包为了方便迁移开发，包含了签名文件。

## 7. 当前用户关心的体验要求

继续开发时需要特别注意以下偏好：

- 用户希望界面尽量贴近当前 WebUI 风格。
- 首页必须一屏显示，不要出现明显滚动。
- 悬浮窗不要占用太多屏幕空间。
- 悬浮窗按钮和文档按钮名称必须与真实 UI 一致。
- 下载界面不要全屏，保持半屏抽屉体验。
- 下载文件删除必须是真实删除本地文件。
- 下载记录要像浏览器一样保存。
- 正常覆盖安装或升级后，下载记录、已保存连接、设置都应该保留。
- 文件名、时间、按钮布局要适配手机屏幕，不能溢出。
- 如果打包给用户，必须使用 release 签名包，不要发 debug 包。

## 8. 近期修改记录

最近完成的修改：

- 新增 release 签名配置。
- 生成 release 签名证书。
- 打包 `AI mini v1.0 release`。
- 下载界面新增分享按钮。
- 下载文件名改为最多两行。
- 下载完成状态和下载时间合并到一行显示。
- 下载记录改为持久化保存。
- 删除下载时同步删除本地文件和下载记录。
- 新增用户文档 `GPTMini-应用介绍与使用帮助.md`。
- 修正文档中悬浮窗按钮名称，和实际 UI 保持一致。

## 9. 建议后续开发事项

可以考虑继续优化：

- 下载记录文件存在性检测。
- 下载记录失效时显示“文件不存在”并允许清理记录。
- release 包版本升级到 `versionCode 2` / `versionName 1.1` 时，保持使用同一个 keystore。
- 为项目补充 Gradle Wrapper，减少新电脑环境配置成本。
- 如果要公开仓库，移除 keystore 和签名密码，只保留签名配置模板。
- 真机测试覆盖：
  - 覆盖安装后下载记录是否保留。
  - 下载后打开、分享、删除是否正常。
  - 多会话通知是否按会话分别更新。
  - 悬浮窗深色/浅色/跟随系统是否一致。
  - 扫码权限首次授权后是否正常进入扫码页。

## 10. 给继续开发 AI 的提示

继续修改前，建议优先阅读：

1. `AI-开发交接说明.md`
2. `GPTMini-应用介绍与使用帮助.md`
3. `app/src/main/java/com/coimgrain/codexminiapp/MainActivity.java`
4. `app/src/main/res/values/strings.xml`
5. `app/build.gradle`

修改 UI 前建议先搜索相关方法：

- `buildWelcomeView`
- `renderDownloads`
- `showDownloadsPanel`
- `buildMiniMenu`
- `showFloatSettings`
- `showNotificationSettings`
- `startHttpDownload`
- `saveDataUrlDownload`
- `persistDownloads`
- `loadPersistedDownloads`

打包前建议执行：

```bash
./scripts/build-debug.sh
./scripts/build-release.sh
```

验证 release APK：

```bash
.tools/android-sdk/build-tools/35.0.0/apksigner verify --verbose "app/build/outputs/apk/release/AI mini-1.0-release.apk"
.tools/android-sdk/build-tools/35.0.0/zipalign -c -p 4 "app/build/outputs/apk/release/AI mini-1.0-release.apk"
```

如果新电脑没有 `.tools`，请改用新电脑 Android SDK 对应的 `apksigner` 和 `zipalign` 路径。

## 6. 多设备（多连接类型）设计

AI mini 从"只能连一种 WebUI"扩展为"多设备容器"：**一个网址 = 一台设备**，设备再带一个**类型**，类型决定标签、配色和产品专属行为。

### 6.1 数据

- 存储于同一个 `SharedPreferences`（`MainActivity.PREFS_NAME`，即 `ai_mini_prefs`）：
  - `saved_connections`：JSON 数组，每项 `{url, name, kind, lastUsedAt}`。
  - `current_device_url` / `current_device_kind`：当前 WebView 里显示的设备（`markCurrentDevice()` 写入）。
  - `last_url`：保留，兼容旧逻辑与"继续上次"。
- **向后兼容**：老记录没有 `kind` 字段，读取时用 `detectDeviceKind(url)` 推断，不会丢设备。
- 类型常量：`DEVICE_KIND_MINIAPP` / `DEVICE_KIND_ZCODE` / `DEVICE_KIND_DSH` / `DEVICE_KIND_OTHER`，顺序见 `DEVICE_KINDS`。

### 6.2 关键方法（都在 MainActivity）

| 方法 | 作用 |
| --- | --- |
| `deviceKindLabel / deviceKindBadge / deviceKindAccent` | 类型的中文名、短标签（Web/ZC/DSH/URL）、强调色 |
| `normalizeDeviceKind / detectDeviceKind` | 类型校验；按 URL 关键字推断（`zcode` → zcode，`dsh`/`deepseek` → dsh，`codex`/`mini` → miniapp） |
| `readSavedConnections / writeSavedConnections / rememberDevice` | 设备读写；`rememberDevice(url, kind)` 显式改类型，`rememberDevice(url, null)` 不会覆盖用户手改的类型 |
| `markCurrentDevice` | 记录当前设备并刷新首页列表与悬浮菜单副标题 |
| `refreshWelcomeDevices / deviceRow` | 首页「我的设备」列表 |
| `showSavedConnectionsDialog / createSavedConnectionRow / showConnectionKindPicker` | 设备管理对话框（切换、重命名、改类型、删除） |
| `currentDeviceSummary` | 悬浮菜单「切换设备」的副标题 |

### 6.3 接入一个新产品的步骤

1. 加类型常量（`DEVICE_KIND_XXX`）并放进 `DEVICE_KINDS`；在 `deviceKindLabel/Badge/Accent` 里补分支。
2. 在 `detectDeviceKind` 里加它的 URL 关键字（可选，用户也可手动选）。
3. 按需加产品专属行为。当前唯一有专属行为的是 `miniapp`：`AIMiniNotificationService.connectionInfo()` 会检查 `current_device_kind`，非 `miniapp` 直接返回 `null`（不轮询 `/codex/status`，只保留常驻通知）。
4. `strings.xml` 补 `device_kind_xxx` 文案。

### 6.4 已知边界

- zcode / dsh 目前**只做连接与切换**，没有任务状态通知、没有页面桥接（`page.js` 里的键盘/下载/任务钩子只对 miniapp WebUI 生效）。
- `page.js` 依赖 UA 里的 `GPTMiniAndroidApp/` 标记判断"是否在 App 内"，改名时**故意没动**；若要为 zcode/dsh 注入各自的桥接脚本，需要按设备类型选择注入内容。

## 7. 多设备通知与页面适配（当前实现）

### 7.1 通知分两条路

| 设备类型 | 状态来源 | 机制 |
| --- | --- | --- |
| 本地 WebUI（`miniapp`） | 服务端轮询 | `AIMiniNotificationService` 轮询 `/codex/status`、`/codex/threads`；`page.js` 同时 hook 页面 fetch 与 `codex:taskstate` 事件做前台快速上报 |
| zcode / dsh / 其它 | 页面侧探测 | `assets/ai-mini/webui-watch.js` 观察页面任务活动，经原生桥上报 `webuiTaskState`，原生直接发通知，**不进入 Codex 轮询** |

已核实的事实：**本地 WebUI（`D:\aicoding\mini\gpt-mini\server.js`）没有 SSE / WebSocket 推送**，路由只有 `/codex/status`、`/codex/threads`（含 `/codex/threads/revision`）、`/codex/gui-status`、`/codex/send` —— 所以轮询是唯一可行方式。
**dsh（DeepSeek Hermes）是 WebSocket 推送**：`packages/api/gateway/src/stream-protocol.ts` 定义 `REMOTE_STREAM_MUX_PATH = '/api/remote.mux'`，帧类型 `ready` / `emit` / `waterfall` / `cancel`（`emit` 即 Host 事件推送）。
**zcode 是 React SPA**（`https://zcode.z.ai/remote/v4/…`，Vite 构建，入口 `assets/index-*.js`），其接口在 bundle 内，尚未完整解析。

### 7.2 通用探测的判定规则（`webui-watch.js`）

- 长 HTTP 请求（fetch/XHR，时长 ≥ 1.5s）→ 任务活动
- WebSocket 消息突发（5s 内 ≥ 4 条）→ 任务活动（覆盖 dsh 这种全程走 WS 的产品）
- 短请求（页面自身轮询、心跳）被忽略，避免误报
- 出现任务活动 → 上报 `running`；运行后静默 4s → 上报 `complete`
- 页面若缺 `viewport` meta 则补一个（窄屏渲染修补）
- 本地 WebUI 页面检测到 `window.__AIMiniGeckoPageBridge` 时只做修补、不探测，避免与 `page.js` 重复上报

阈值都写在文件顶部常量里（`LONG_REQUEST_MS` / `WS_BURST` / `WS_WINDOW_MS` / `IDLE_DONE_MS`），真机误报时优先调这几个值。

### 7.3 各产品的实测结论（本轮核实）

| 产品 | 状态通道 | 我们的接入方式 |
| --- | --- | --- |
| 本地 WebUI | HTTP 轮询 `/codex/status`、`/codex/threads`（**无 SSE/WebSocket**，已核实 `server.js` 路由） | 服务端轮询 + `page.js` 前台 hook（精确） |
| dsh | WebSocket `/api/remote.mux`，JSON 文本帧 `{type:'emit',event,args}`；事件 `api-session/status` 的 `args[1]` 就是"该会话是否在跑"（已核实 `packages/client/ui-session/src/client/index.ts` 的 `$on('api-session/status', (sessionId, running) => …)`） | **精确**：`webui-watch.js` 解析该事件直接判定 running/complete |
| zcode | 二进制 RPC（bundle 内 `ChannelClient` / `protocol.send(buffer)` / `onBuffer` 编解码，形似 vscode-jsonrpc 的二进制变体），bundle 里没有 `new WebSocket(` 或 `/api/...` 明文端点 | **兜底**：DOM「停止」按钮 + 长请求 + WS 突发三条启发式 |

zcode 的协议要在注入脚本里复刻编解码不划算，所以维持兜底方案；它同样是 React SPA，DOM 里存在"停止/中断"按钮，DOM 判据通常就能命中。

顺带确认：**dsh 前端是响应式设计**（`@container (max-width: 560px)`、`--dsh-composer-card-max-width: min(calc(100% - 16px), 952px)`），手机上不需要额外布局修补；zcode 的 UI 本身自带"流光"组件（bundle 里的 `--beam-strength`、`hueRange`、`colorVariant: 'colorful'|'mono'`），与我们在设备卡片上给的"黑白流光"配色一致。

### 7.5 注入策略与安全区（重要，曾踩坑）

**注入按设备类型分派**（`AIMiniBrowserEngine.documentStartScript`）：

| 设备类型 | page.js | webui-watch.js | 原生桥 |
| --- | --- | --- | --- |
| `miniapp`（GPT） | ✅ 注入 | ✅（检测到 page.js 后只做修补） | ✅ |
| `zcode` / `dsh` / `other` | ❌ **不注入** | ✅ | ✅ |

`page.js` 是给 GPT 定制的（液态玻璃样式、字体、键盘钩子、任务钩子、下载钩子）。**早期版本把它无差别注入到所有设备**，导致 dsh / zcode 的页面被注入样式与钩子，看起来"变成了 GPT 的界面"——现已按类型隔离。新增产品时如果发现界面被改造，先检查这里。

**状态栏安全区**：GPT 的页面用 CSS 变量 `--ai-mini-top-inset` 自己把顶部功能栏下移（所以 WebView 可以延伸到状态栏下方）。zcode / dsh 等页面不知道这个变量，且它们的顶栏多为 `fixed/sticky`，往页面里注 padding 压不下去 —— 因此由原生在 `browserFrame` 顶部留出状态栏高度（`applyDeviceTopInset()` / `statusBarHeightPx()`），状态栏区域用深色 `rgb(11,18,32)` 与两个产品的深色主题衔接；切回 GPT 设备时自动恢复为 0。

**设备名与徽标**：`miniapp` 的中文名是「GPT」，徽标缩写 `GPT`（zcode → `ZC`，dsh → `DSH`，其它 → `URL`）。

**悬浮窗切换列表只做切换**：`deviceRow(..., withActions=false)` 不显示 ⋯、不挂长按菜单；重命名 / 改类型 / 删除统一在首页的设备卡片上做（⋯ 或长按）。

### 7.6 设备认证与 cookie（以 dsh 为例）

dsh 的 WebUI 首次访问会 **303 See Other** 到 `./`，同时下发认证 cookie（实测自 `http://223.113.43.55:7788/?token=…`）：

```
set-cookie: dsh-auth-<随机串>=v1.<payload>.<sig>; Max-Age=2592000; Path=/; HttpOnly; SameSite=Strict
```

（30 天有效期、HttpOnly、SameSite=Strict）

| 请求 | 结果 |
| --- | --- |
| 带 token 访问 `/?token=…` | 303 → 下发 cookie → 跟随重定向后 200，正常显示界面 |
| 带 cookie 访问 `/` | 200，正常显示界面 |
| 不带 cookie 访问 `/` | 401 `dsh web authentication required; reopen the URL printed by dsh web.` |

所以 App 侧必须做到两件事，否则表现为"卡在认证页"：

| 措施 | 实现 |
| --- | --- |
| cookie 落盘 | `flushWebCookies()` 在 `onPageFinished` 与 `onPause` 调用 `CookieManager.flush()` —— Android 的 cookie 默认只在内存，不 flush 时进程被系统回收就丢 |
| 接受持久/第三方 cookie | `AIMiniBrowserView` 初始化：`setAcceptCookie(true)` + `setAcceptThirdPartyCookies(webView, true)` |
| 入口地址保留 token | `inheritMainNavigationToken()` 让页面内跳转不丢 token；`restoreAuthParamsIfNeeded()` 在入口地址缺 token 时，用同一主机上保存过的带 token 地址补回 |

### 7.7 发版流程（版本号每次 +1）

1. 改 `app/build.gradle`：`versionName` +1（`1.0.1` → `1.0.2`）、`versionCode` +1、`archivesBaseName` 同步
2. `.\build-apk.ps1 -Project D:\aicoding\AImini\GPT-Mini-Android -Task assembleRelease`
3. 推送：`git push aimini main`（机场节点全挂时用 `git -c http.proxy= -c https.proxy= push aimini main` 直连，实测可用）
4. 发布：`gh release create v1.0.2 "app\build\outputs\apk\release\AI mini-1.0.2-release.apk" --repo 3354282339/AI-mini --title "AI mini 1.0.2" --notes-file notes.md`
   - ⚠️ GitHub 会把上传文件名里的**空格换成点**（`AI mini-…` → `AI.mini-…`），上传后用 `gh api -X PATCH repos/3354282339/AI-mini/releases/assets/<id> -f name="AI-mini-1.0.2-release.apk"` 改回
5. 校验：`gh release download` 拉回来比对 SHA256

「检查更新」读取 `https://api.github.com/repos/3354282339/AI-mini/releases`，取最新 release 的 `.apk` 资产（优先名字含 `webview`，否则任意 `.apk`，排除 `gecko`），与当前 `versionName` 比较后提示；自动检查静默失败，手动打开更新页会显示错误。

### 7.8 真机调试入口
页面里注入了探针函数，可用 WebView 调试或在代码里 `evaluateJavascript` 读取：

```js
window.__AIMiniWebUiWatchProbe()
// { deviceKind:'dsh', running:true, precise:true, domBusy:false, active:0, idleMs:312 }
```

- `precise:true` 表示已收到产品精确帧（dsh 的 `api-session/status`），此时启发式已关闭
- `precise:false` 时看 `domBusy` / `active` / `idleMs` 判断当前走的是哪条启发式
- 阈值在 `webui-watch.js` 顶部：`LONG_REQUEST_MS` / `WS_BURST` / `WS_WINDOW_MS` / `IDLE_DONE_MS`

### 7.5 仍需真机反馈的部分

1. dsh / zcode 里跑一次任务，确认完成通知是否准时、有无误报（`precise` 是否变 true）。
2. 两个页面在手机上的实际观感（横向溢出、输入框被键盘遮挡、按钮热区）。
3. zcode 若要精确通知，需要抓一次它二进制 RPC 的实际帧（真机 + WebView 调试），或者退一步用它的 DOM 状态文本。



