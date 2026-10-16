/**
 * AI mini —— 通用 WebUI 任务监视（注入脚本）。
 *
 * 用途：给「非本地 WebUI」的设备（zcode、dsh、其它）提供任务运行/完成探测。
 * 这类产品的页面协议各不相同（dsh 走 WebSocket /api/remote.mux 推送，zcode 未知），
 * 与其为每个产品写死协议，不如从页面侧观察「任务活动」：
 *
 *   1. 长时间未结束的 HTTP 请求（> LONG_REQUEST_MS）—— 多数 WebUI 的对话请求是长请求；
 *   2. 短时间内的 WebSocket 消息突发（连续 >= WS_BURST 条且间隔 < WS_WINDOW_MS）
 *      —— 覆盖 dsh 那种全程走 WS 流式推送的产品；
 *   3. 极短请求（轮询、心跳）被忽略，避免把页面自身的定时刷新当成任务。
 *
 * 判定：出现任务活动 -> running；运行中之后静默 IDLE_DONE_MS -> complete。
 * 结果通过 AIMiniNative.postMessage 上报，类型为 webuiTaskState。
 *
 * 另外做一件轻量的移动端修补：页面若缺 viewport meta 就补一个，避免窄屏按桌面宽度渲染。
 *
 * 注意：本地 WebUI（miniapp）已由 page.js 精确接管，本脚本在那种页面上只做修补、不探测，
 * 以免两套逻辑重复上报。
 */
(function () {
  "use strict";
  if (window.__AIMiniWebUiWatch) return;
  window.__AIMiniWebUiWatch = true;

  var MANAGED_BY_PAGE_JS = !!window.__AIMiniGeckoPageBridge;

  var LONG_REQUEST_MS = 1500;   // 超过这个时长的请求才算任务活动
  var WS_BURST = 4;             // 连续 WS 消息条数阈值
  var WS_WINDOW_MS = 5000;      // WS 突发窗口
  var IDLE_DONE_MS = 4000;      // 运行中之后静默多久算完成
  var TICK_MS = 700;

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

  function markActivity() {
    lastActivityAt = Date.now();
    if (!running) {
      running = true;
      runStartedAt = Date.now();
      emit("running");
    }
    startTicker();
  }

  function startTicker() {
    if (ticker) return;
    ticker = setInterval(function () {
      if (!running) return;
      if (activeRequests > 0) {
        lastActivityAt = Date.now();
        return;
      }
      if (Date.now() - lastActivityAt >= IDLE_DONE_MS) {
        running = false;
        clearInterval(ticker);
        ticker = null;
        emit("complete");
      }
    }, TICK_MS);
  }

  function onRequestStart() {
    activeRequests += 1;
  }

  function onRequestEnd(durationMs) {
    activeRequests = Math.max(0, activeRequests - 1);
    if (durationMs >= LONG_REQUEST_MS) {
      markActivity();
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
            markActivity();
          } else if (running) {
            lastActivityAt = now;
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

  installViewportMeta();

  if (MANAGED_BY_PAGE_JS) return;

  installFetchHook();
  installXhrHook();
  installWebSocketHook();
  window.__AIMiniWebUiWatchProbe = function () {
    return { running: running, active: activeRequests, idleMs: Date.now() - lastActivityAt };
  };
})();
