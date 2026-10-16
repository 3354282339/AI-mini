/**
 * AI mini —— 通用 WebUI 任务监视（注入脚本）。
 *
 * 给「非本地 WebUI」的设备（zcode、dsh、其它）提供任务运行/完成探测。
 * 这类产品协议各不相同，因此按可靠性从高到低用三种判据：
 *
 *   1. DOM 特征（主判据）：页面上出现「停止生成」这类"打断当前任务"的按钮，
 *      就说明任务正在跑；按钮消失即认为结束。dsh 的该按钮文案已从源码核实
 *      （packages/client/ui-conversation/src/client/locales.ts: 'input.stop' = '停止生成'）。
 *   2. WebSocket 消息突发：5 秒内 ≥ 4 条消息算任务活动（覆盖 dsh 那种全程走 WS 的产品）。
 *   3. 长 HTTP 请求：时长 ≥ 1.5s 的 fetch/XHR 算任务活动。
 *
 * 短请求（页面自身轮询、心跳）被忽略，避免把页面刷新当成任务。
 * 判定结果通过 AIMiniNative.postMessage 上报，类型 webuiTaskState（running / complete）。
 *
 * 另外做一件轻量修补：页面若缺 viewport meta 就补一个，避免窄屏按桌面宽度渲染。
 *
 * 注意：本地 WebUI（miniapp）已由 page.js 精确接管，本脚本在那种页面上只做修补、不探测，
 * 以免两套逻辑重复上报。
 */
(function () {
  "use strict";
  if (window.__AIMiniWebUiWatch) return;
  window.__AIMiniWebUiWatch = true;

  var MANAGED_BY_PAGE_JS = !!window.__AIMiniGeckoPageBridge;

  /** 运行中才会出现的按钮文案（中英），命中即视为任务在跑。 */
  var BUSY_TEXTS = [
    "停止生成",
    "停止响应",
    "停止输出",
    "停止回答",
    "停止运行",
    "生成中",
    "正在生成",
    "思考中",
    "stop generating",
    "stop response",
    "stop output",
    "stop the run",
    "generating"
  ];
  /** 按钮特征词（属性层），与 BUSY_TEXTS 组合判断。 */
  var BUSY_ATTRIBUTE_HINTS = ["stop", "interrupt", "cancel"];
  var BUSY_ATTRIBUTE_TARGETS = ["generat", "response", "run", "stream", "agent", "task"];

  var LONG_REQUEST_MS = 1500;
  var WS_BURST = 4;
  var WS_WINDOW_MS = 5000;
  var IDLE_DONE_MS = 4000;
  var TICK_MS = 800;

  var activeRequests = 0;
  var running = false;
  var lastActivityAt = 0;
  var runStartedAt = 0;
  var wsBurst = 0;
  var wsLastAt = 0;
  var ticker = null;

  function post(message) {
    try {
      if (window.AIMiniNative && window.AIMiniNative.postMessage) {
        window.AIMiniNative.postMessage(JSON.stringify(message));
      }
    } catch (error) {
    }
  }

  function pageTitle() {
    var title = String(document.title || "").replace(/\s+/g, " ").trim();
    return title || "WebUI";
  }

  function emit(status) {
    post({
      type: "webuiTaskState",
      threadId: "",
      threadName: pageTitle(),
      status: status,
      statusUrl: String(location.href || ""),
      startedAt: runStartedAt > 0 ? runStartedAt : Date.now()
    });
  }

  /** 页面上是否存在「可打断当前任务」的按钮 —— 最可靠的运行中信号。 */
  function domBusy() {
    try {
      var nodes = document.querySelectorAll(
        'button,[role="button"],[aria-label],[data-testid],[title]'
      );
      for (var index = 0; index < nodes.length; index++) {
        var node = nodes[index];
        if (node.offsetParent === null && node.getClientRects().length === 0) continue;
        var attributeText = String(
          (node.getAttribute("aria-label") || "")
          + " "
          + (node.getAttribute("data-testid") || "")
          + " "
          + (node.getAttribute("title") || "")
        ).toLowerCase();
        var text = String(node.textContent || "").replace(/\s+/g, " ").trim();
        var haystack = (attributeText + " " + text).toLowerCase();
        if (!haystack.trim()) continue;

        for (var t = 0; t < BUSY_TEXTS.length; t++) {
          if (haystack.indexOf(BUSY_TEXTS[t].toLowerCase()) >= 0) return true;
        }
        var hasHint = false;
        for (var h = 0; h < BUSY_ATTRIBUTE_HINTS.length; h++) {
          if (attributeText.indexOf(BUSY_ATTRIBUTE_HINTS[h]) >= 0) {
            hasHint = true;
            break;
          }
        }
        if (!hasHint) continue;
        for (var g = 0; g < BUSY_ATTRIBUTE_TARGETS.length; g++) {
          if (attributeText.indexOf(BUSY_ATTRIBUTE_TARGETS[g]) >= 0) return true;
        }
      }
    } catch (error) {
    }
    return false;
  }

  function wsRecentlyActive() {
    return wsLastAt > 0 && Date.now() - wsLastAt < WS_WINDOW_MS;
  }

  function startTicker() {
    if (ticker) return;
    ticker = setInterval(tick, TICK_MS);
  }

  function tick() {
    var busy = domBusy();
    if (busy) {
      lastActivityAt = Date.now();
      if (!running) {
        running = true;
        runStartedAt = Date.now();
        emit("running");
      }
      return;
    }
    // 没有"停止"按钮时，仍用网络活动兜底（有些产品不暴露打断按钮）。
    if (activeRequests > 0 || wsRecentlyActive()) {
      lastActivityAt = Date.now();
      if (!running) {
        running = true;
        runStartedAt = Date.now();
        emit("running");
      }
      return;
    }
    if (running && Date.now() - lastActivityAt >= IDLE_DONE_MS) {
      running = false;
      emit("complete");
    }
  }

  function onRequestStart() {
    activeRequests += 1;
  }

  function onRequestEnd(durationMs) {
    activeRequests = Math.max(0, activeRequests - 1);
    if (durationMs >= LONG_REQUEST_MS) {
      lastActivityAt = Date.now();
      startTicker();
    }
  }

  function installFetchHook() {
    if (!window.fetch) return;
    var original = window.fetch;
    window.fetch = function () {
      var startedAt = Date.now();
      onRequestStart();
      var result;
      try {
        result = original.apply(this, arguments);
      } catch (error) {
        onRequestEnd(Date.now() - startedAt);
        throw error;
      }
      if (result && typeof result.then === "function") {
        return result.then(function (response) {
          onRequestEnd(Date.now() - startedAt);
          return response;
        }, function (error) {
          onRequestEnd(Date.now() - startedAt);
          throw error;
        });
      }
      onRequestEnd(Date.now() - startedAt);
      return result;
    };
  }

  function installXhrHook() {
    if (!window.XMLHttpRequest || !XMLHttpRequest.prototype) return;
    var originalSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function () {
      var startedAt = Date.now();
      var xhr = this;
      onRequestStart();
      try {
        xhr.addEventListener("loadend", function () {
          onRequestEnd(Date.now() - startedAt);
        }, { once: true });
      } catch (error) {
      }
      return originalSend.apply(this, arguments);
    };
  }

  function installWebSocketHook() {
    if (!window.WebSocket) return;
    var Original = window.WebSocket;
    function Wrapped(url, protocols) {
      var socket = protocols === undefined ? new Original(url) : new Original(url, protocols);
      try {
        socket.addEventListener("message", function () {
          var now = Date.now();
          wsBurst = now - wsLastAt < WS_WINDOW_MS ? wsBurst + 1 : 1;
          wsLastAt = now;
          if (wsBurst >= WS_BURST) {
            lastActivityAt = now;
            startTicker();
          }
        });
      } catch (error) {
      }
      return socket;
    }
    Wrapped.prototype = Original.prototype;
    Wrapped.CONNECTING = Original.CONNECTING;
    Wrapped.OPEN = Original.OPEN;
    Wrapped.CLOSING = Original.CLOSING;
    Wrapped.CLOSED = Original.CLOSED;
    window.WebSocket = Wrapped;
  }

  function installViewportMeta() {
    try {
      if (document.querySelector('meta[name="viewport"]')) return;
      var head = document.head || document.documentElement;
      if (!head) return;
      var meta = document.createElement("meta");
      meta.setAttribute("name", "viewport");
      meta.setAttribute("content", "width=device-width, initial-scale=1, viewport-fit=cover");
      head.appendChild(meta);
    } catch (error) {
    }
  }

  /**
   * 基础移动端修补：未适配的 WebUI（zcode / dsh 等）在手机宽度下常见两类问题 ——
   * 横向溢出（元素超出视口导致整页可左右拖动）和文本自动放大。
   * 只做这两条最保守的修正，不动字体大小与布局结构，避免破坏产品自身样式。
   */
  function installMobileCss() {
    try {
      if (document.getElementById("ai-mini-mobile-fix")) return;
      var style = document.createElement("style");
      style.id = "ai-mini-mobile-fix";
      style.textContent =
        "html,body{max-width:100%;overflow-x:hidden;}"
        + "body{-webkit-text-size-adjust:100%;text-size-adjust:100%;}";
      (document.head || document.documentElement).appendChild(style);
    } catch (error) {
    }
  }

  installViewportMeta();
  installMobileCss();

  if (MANAGED_BY_PAGE_JS) return;

  installFetchHook();
  installXhrHook();
  installWebSocketHook();
  startTicker();
  window.__AIMiniWebUiWatchProbe = function () {
    return {
      running: running,
      domBusy: domBusy(),
      active: activeRequests,
      idleMs: Date.now() - lastActivityAt
    };
  };
})();
