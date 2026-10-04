const API_BASE = "/api";
const AUTH_KEY = "agentx.auth";
const AGENT_KEY = "agentx.activeAgent";
const SESSION_KEY = "agentx.activeSession";

const app = document.querySelector("#app");
const toastRegion = document.querySelector("#toast-region");

const icons = {
  alert: '<circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/>',
  bot: '<path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  chevronDown: '<path d="m6 9 6 6 6-6"/>',
  key: '<circle cx="7.5" cy="15.5" r="3.5"/><path d="m10.5 12.5 8-8"/><path d="m15 8 2 2"/><path d="m18 5 2 2"/>',
  lock: '<rect width="18" height="11" x="3" y="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
  logOut: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/>',
  menu: '<line x1="4" x2="20" y1="12" y2="12"/><line x1="4" x2="20" y1="6" y2="6"/><line x1="4" x2="20" y1="18" y2="18"/>',
  message: '<path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"/>',
  pencil: '<path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 4 4"/>',
  plus: '<path d="M5 12h14"/><path d="M12 5v14"/>',
  refresh: '<path d="M20 11a8.1 8.1 0 0 0-15.5-2M4 4v5h5"/><path d="M4 13a8.1 8.1 0 0 0 15.5 2M20 20v-5h-5"/>',
  save: '<path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2Z"/><path d="M17 21v-8H7v8"/><path d="M7 3v5h8"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  send: '<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',
  stop: '<rect width="12" height="12" x="6" y="6" rx="2" fill="currentColor" stroke="none"/>',
  trash: '<path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/>',
  user: '<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  x: '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>',
};

const state = {
  auth: readJson(AUTH_KEY),
  agents: [],
  apiKeys: [],
  sessions: [],
  messages: [],
  activeAgentId: sessionStorage.getItem(AGENT_KEY) || "",
  activeSessionId: sessionStorage.getItem(SESSION_KEY) || "",
  view: "chat",
  modal: null,
  loginBusy: false,
  workspaceBusy: false,
  agentBusy: false,
  apiKeyBusy: false,
  messagesBusy: false,
  chatBusy: false,
  sidebarOpen: false,
  pendingReply: false,
  streamingContent: "",
  streamAbortController: null,
  streamMetadata: createEmptyStreamMetadata(),
  streamStartedAt: 0,
};

class ApiError extends Error {
  constructor(message, code = "") {
    super(message);
    this.name = "ApiError";
    this.code = code;
  }
}

function svgIcon(name, size = 18, className = "") {
  const paths = icons[name] || icons.alert;
  return `<svg class="${className}" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function readJson(key) {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

function createEmptyStreamMetadata() {
  return {
    messageId: "",
    model: "",
    provider: "",
    promptTokens: null,
    completionTokens: null,
    totalTokens: null,
    usageSource: "",
    finishReason: "",
    latencyMs: null,
  };
}

function parseStreamPayload(data) {
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

function estimateTokenCount(text) {
  const value = String(text || "");
  if (!value) {
    return 0;
  }
  const cjkMatches = value.match(/[\u3400-\u9fff\uf900-\ufaff\u3040-\u30ff\uac00-\ud7af]/g) || [];
  const remainder = value.replace(/[\u3400-\u9fff\uf900-\ufaff\u3040-\u30ff\uac00-\ud7af]/g, "");
  return cjkMatches.length + Math.ceil(remainder.length / 4);
}

function clearAuth() {
  resetStreamingState();
  localStorage.removeItem(AUTH_KEY);
  sessionStorage.removeItem(AGENT_KEY);
  sessionStorage.removeItem(SESSION_KEY);
  state.auth = null;
  state.agents = [];
  state.apiKeys = [];
  state.sessions = [];
  state.messages = [];
  state.activeAgentId = "";
  state.activeSessionId = "";
  state.view = "chat";
  state.modal = null;
}

async function apiRequest(path, options = {}) {
  const {
    method = "GET",
    body,
    authenticated = true,
  } = options;
  const headers = {};

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (authenticated && state.auth?.token) {
    headers.Authorization = `Bearer ${state.auth.token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError("无法连接后端服务，请确认 8091 端口已经启动");
  }

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (response.status === 401) {
    throw new ApiError(payload?.info || "登录已过期，请重新登录", "401");
  }

  if (!response.ok) {
    throw new ApiError(payload?.info || `请求失败，HTTP ${response.status}`, String(response.status));
  }

  if (payload?.code && payload.code !== "0000") {
    throw new ApiError(payload.info || "请求失败", payload.code);
  }

  return payload?.data;
}

function cancelActiveStream() {
  if (state.streamAbortController) {
    state.streamAbortController.abort();
    state.streamAbortController = null;
  }
}

function resetStreamingState() {
  cancelActiveStream();
  state.chatBusy = false;
  state.pendingReply = false;
  state.streamingContent = "";
  state.streamMetadata = createEmptyStreamMetadata();
  state.streamStartedAt = 0;
}

function parseSseEvent(block) {
  let event = "message";
  const data = [];

  for (const line of block.split(/\r?\n/)) {
    if (!line || line.startsWith(":")) {
      continue;
    }

    const separator = line.indexOf(":");
    const field = separator === -1 ? line : line.slice(0, separator);
    let value = separator === -1 ? "" : line.slice(separator + 1);
    if (value.startsWith(" ")) {
      value = value.slice(1);
    }

    if (field === "event") {
      event = value;
    } else if (field === "data") {
      data.push(value);
    }
  }

  return {
    event,
    data: data.join("\n"),
  };
}

async function streamApiRequest(path, body, onEvent, signal) {
  const headers = {
    Accept: "text/event-stream",
    "Content-Type": "application/json",
  };

  if (state.auth?.token) {
    headers.Authorization = `Bearer ${state.auth.token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers,
      body: JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error.name === "AbortError") {
      throw error;
    }
    throw new ApiError("无法连接后端服务，请确认 8091 端口已经启动");
  }

  if (response.status === 401) {
    const payload = await response.json().catch(() => null);
    throw new ApiError(payload?.info || "登录已过期，请重新登录", "401");
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new ApiError(payload?.info || `请求失败，HTTP ${response.status}`, String(response.status));
  }

  if (!response.body) {
    throw new ApiError("当前浏览器不支持流式响应");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  const dispatch = (block) => {
    const sseEvent = parseSseEvent(block);
    if (sseEvent.event === "error") {
      throw new ApiError(sseEvent.data || "流式对话失败");
    }
    onEvent(sseEvent);
  };

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      let boundary = buffer.match(/\r?\n\r?\n/);
      while (boundary && boundary.index !== undefined) {
        const block = buffer.slice(0, boundary.index);
        buffer = buffer.slice(boundary.index + boundary[0].length);
        if (block.trim()) {
          dispatch(block);
        }
        boundary = buffer.match(/\r?\n\r?\n/);
      }
    }

    buffer += decoder.decode();
    if (buffer.trim()) {
      dispatch(buffer);
    }
  } finally {
    reader.releaseLock();
  }
}

function activeAgent() {
  return state.agents.find((agent) => agent.id === state.activeAgentId) || null;
}

function activeSession() {
  return state.sessions.find((session) => session.id === state.activeSessionId) || null;
}

function apiKeyById(apiKeyId) {
  return state.apiKeys.find((apiKey) => apiKey.id === apiKeyId) || null;
}

function apiKeyUsageCount(apiKeyId) {
  return state.agents.filter((agent) => agent.apiKeyId === apiKeyId).length;
}

function apiKeyDisplayName(apiKey) {
  return apiKey?.name || providerLabel(apiKey?.provider) || "未命名密钥";
}

function providerLabel(provider) {
  if (provider === "openai") {
    return "OpenAI 兼容";
  }
  if (provider === "anthropic") {
    return "Anthropic";
  }
  return provider || "未配置";
}

function persistSelection() {
  if (state.activeAgentId) {
    sessionStorage.setItem(AGENT_KEY, state.activeAgentId);
  } else {
    sessionStorage.removeItem(AGENT_KEY);
  }

  if (state.activeSessionId) {
    sessionStorage.setItem(SESSION_KEY, state.activeSessionId);
  } else {
    sessionStorage.removeItem(SESSION_KEY);
  }
}

function initials(value) {
  const text = String(value || "A").trim();
  if (!text) {
    return "A";
  }
  return text.slice(0, 2).toUpperCase();
}

function formatSessionTime(value) {
  if (!value) {
    return "刚刚";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "刚刚";
  }

  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  if (isToday) {
    return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
  }
  return date.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" });
}

function formatMessageTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
}

function formatTokenCount(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "0";
  }
  return Math.max(0, Math.trunc(number)).toLocaleString("zh-CN");
}

function sessionTokenSummary(session, compact = false) {
  if (!session) {
    return "";
  }
  const used = Number(session.usedTokens || 0);
  if (session.tokenLimit === null || session.tokenLimit === undefined) {
    return compact ? `已用 ${formatTokenCount(used)} Token` : `Token 不限 · 已用 ${formatTokenCount(used)}`;
  }
  const limit = Number(session.tokenLimit);
  const remaining = Math.max(limit - used, 0);
  return compact
    ? `${formatTokenCount(used)} / ${formatTokenCount(limit)} Token`
    : `${formatTokenCount(used)} / ${formatTokenCount(limit)} Token · 剩余 ${formatTokenCount(remaining)}`;
}

function isSessionTokenExhausted(session) {
  if (!session || session.tokenLimit === null || session.tokenLimit === undefined) {
    return false;
  }
  return Number(session.usedTokens || 0) >= Number(session.tokenLimit);
}

function applySessionTokenBudget(payload) {
  if (!payload || !state.activeSessionId) {
    return;
  }
  state.sessions = state.sessions.map((session) =>
    session.id === state.activeSessionId
      ? {
          ...session,
          tokenLimit: payload.tokenLimit ?? null,
          usedTokens: payload.usedTokens ?? session.usedTokens ?? 0,
        }
      : session,
  );
}

function showToast(type, title, message = "") {
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  const iconName = type === "error" ? "alert" : type === "info" ? "alert" : "check";
  toast.innerHTML = `
    ${svgIcon(iconName, 18)}
    <div class="toast-copy">
      <div class="toast-title">${escapeHtml(title)}</div>
      ${message ? `<div class="toast-message">${escapeHtml(message)}</div>` : ""}
    </div>
  `;
  toastRegion.append(toast);
  window.setTimeout(() => toast.remove(), 4300);
}

function renderLogin(error = "") {
  const username = state.auth?.username || "admin";
  app.innerHTML = `
    <main class="login-page">
      <section class="login-panel" aria-labelledby="login-title">
        <div class="login-brand">
          <span class="brand-mark">AX</span>
          <div>
            <div class="brand-name">AgentX</div>
            <div class="brand-caption">Workspace</div>
          </div>
        </div>
        <h1 class="login-title" id="login-title">登录工作台</h1>
        <p class="login-subtitle">进入会话、历史消息和 Agent 工作区</p>
        <form id="login-form" novalidate>
          <div class="field">
            <label for="login-username">用户名</label>
            <div class="input-with-icon">
              ${svgIcon("user", 17)}
              <input
                id="login-username"
                name="username"
                value="${escapeHtml(username)}"
                autocomplete="username"
                maxlength="64"
                required
              />
            </div>
          </div>
          <div class="field">
            <label for="login-password">密码</label>
            <div class="input-with-icon">
              ${svgIcon("lock", 17)}
              <input
                id="login-password"
                name="password"
                type="password"
                autocomplete="current-password"
                maxlength="128"
                required
                autofocus
              />
            </div>
          </div>
          <p class="form-error" role="alert">${error ? svgIcon("alert", 15) + escapeHtml(error) : ""}</p>
          <button class="primary-button wide" type="submit" ${state.loginBusy ? "disabled" : ""}>
            ${state.loginBusy ? '<span class="button-spinner" aria-hidden="true"></span>' : ""}
            <span>${state.loginBusy ? "正在登录" : "登录"}</span>
          </button>
        </form>
        <div class="login-meta">
          <span><span class="status-dot"></span>本地演示环境</span>
          <span>JWT 安全认证</span>
        </div>
      </section>
    </main>
  `;
}

function renderAgentPicker() {
  if (!state.agents.length) {
    return `
      <div class="agent-picker">
        <select disabled><option>暂无 Agent</option></select>
        ${svgIcon("chevronDown", 16)}
      </div>
    `;
  }

  return `
    <div class="agent-picker">
      <label class="sr-only" for="agent-select">选择 Agent</label>
      <select id="agent-select" data-agent-select>
        ${state.agents
          .map(
            (agent) => `
              <option value="${escapeHtml(agent.id)}" ${agent.id === state.activeAgentId ? "selected" : ""}>
                ${escapeHtml(agent.name || "未命名 Agent")}
              </option>
            `,
          )
          .join("")}
      </select>
      ${svgIcon("chevronDown", 16)}
    </div>
  `;
}

function renderSessions() {
  if (state.workspaceBusy && !state.sessions.length) {
    return `
      <div class="sidebar-empty">
        <span class="spinner small"></span>
        <div>正在加载会话</div>
      </div>
    `;
  }

  if (!state.sessions.length) {
    return '<div class="sidebar-empty">暂无会话</div>';
  }

  return state.sessions
    .map(
      (session) => `
        <div
          class="session-item ${session.id === state.activeSessionId ? "active" : ""}"
          data-action="select-session"
          data-session-id="${escapeHtml(session.id)}"
          role="button"
          tabindex="0"
        >
          <div class="session-title">${escapeHtml(session.title || "新会话")}</div>
          <div class="session-time">
            ${escapeHtml(formatSessionTime(session.updateTime || session.createTime))}
            · ${escapeHtml(sessionTokenSummary(session, true))}
          </div>
          <div class="session-actions">
            <button
              class="icon-button"
              type="button"
              data-action="rename-session"
              data-session-id="${escapeHtml(session.id)}"
              title="重命名"
              aria-label="重命名会话"
            >${svgIcon("pencil", 15)}</button>
            <button
              class="icon-button danger"
              type="button"
              data-action="delete-session"
              data-session-id="${escapeHtml(session.id)}"
              title="删除"
              aria-label="删除会话"
            >${svgIcon("trash", 15)}</button>
          </div>
        </div>
      `,
    )
    .join("");
}

function renderMessage(message, pending = false) {
  const isUser = message.role === "user";
  const content = pending
    ? message.content
      ? `${escapeHtml(message.content)}<span class="streaming-cursor" aria-hidden="true"></span>`
      : '<span class="typing-dots" aria-label="正在生成回复"><span></span><span></span><span></span></span>'
    : escapeHtml(message.content);
  const stats = isUser ? "" : renderMessageStats(message);

  return `
    <article class="message-row ${isUser ? "user" : "assistant"} ${pending ? "pending" : ""}">
      <div class="message-avatar">
        ${isUser ? svgIcon("user", 16) : svgIcon("bot", 17)}
      </div>
      <div class="message-content">
        <div class="message-meta">
          <span class="message-role">${isUser ? "你" : "Agent"}</span>
          <span>${escapeHtml(formatMessageTime(message.createTime))}</span>
        </div>
        <div class="message-bubble">${content}</div>
        ${stats}
      </div>
    </article>
  `;
}

function renderMessageStats(message) {
  const items = [];
  if (message.model) {
    items.push({ text: message.model });
  }

  if (message.totalTokens !== null && message.totalTokens !== undefined) {
    const tokenTitle = [
      message.promptTokens !== null && message.promptTokens !== undefined
        ? `输入 ${message.promptTokens}`
        : "",
      message.completionTokens !== null && message.completionTokens !== undefined
        ? `输出 ${message.completionTokens}`
        : "",
    ]
      .filter(Boolean)
      .join(" / ");
    const estimated = message.usageSource === "estimated" ? "预估 " : "";
    items.push({
      text: `${estimated}${message.totalTokens} Token`,
      title: tokenTitle,
    });
  }

  if (message.latencyMs !== null && message.latencyMs !== undefined) {
    items.push({ text: `${(message.latencyMs / 1000).toFixed(1)}s` });
  }

  if (message.finishReason === "cancelled") {
    items.push({ text: "已停止" });
  } else if (message.finishReason === "error") {
    items.push({ text: "生成失败" });
  }

  if (!items.length) {
    return "";
  }
  return `
    <div class="message-stats">
      ${items
        .map(
          (item) =>
            `<span${item.title ? ` title="${escapeHtml(item.title)}"` : ""}>${escapeHtml(item.text)}</span>`,
        )
        .join("")}
    </div>
  `;
}

function renderChatContent() {
  const agent = activeAgent();
  const session = activeSession();

  if (!state.agents.length) {
    return `
      <div class="empty-state">
        <div class="empty-icon">${svgIcon("bot", 24)}</div>
        <h2>暂无可用 Agent</h2>
        <p>当前账号下还没有 Agent，刷新后重新检查数据。</p>
        <button class="secondary-button" type="button" data-action="refresh">
          ${svgIcon("refresh", 16)}
          <span>刷新</span>
        </button>
      </div>
    `;
  }

  if (!session) {
    const welcome = agent?.welcomeMessage || "选择一个会话，或创建一段新对话。";
    return `
      <div class="empty-state">
        <div class="empty-icon">${svgIcon("message", 23)}</div>
        <h2>${escapeHtml(agent?.name || "Agent")}</h2>
        <p>${escapeHtml(welcome)}</p>
        <button class="primary-button" type="button" data-action="new-session" ${state.workspaceBusy ? "disabled" : ""}>
          ${svgIcon("plus", 16)}
          <span>新建会话</span>
        </button>
      </div>
    `;
  }

  if (state.messagesBusy && !state.messages.length) {
    return `
      <div class="empty-state">
        <span class="spinner"></span>
        <p>正在加载历史消息</p>
      </div>
    `;
  }

  if (!state.messages.length && !state.pendingReply) {
    return `
      <div class="empty-state">
        <div class="empty-icon">${svgIcon("message", 23)}</div>
        <h2>${escapeHtml(session.title || agent?.name || "新会话")}</h2>
        <p>${escapeHtml(agent?.welcomeMessage || "从第一条消息开始这段对话。")}</p>
      </div>
    `;
  }

  return `
    ${state.messages.map((message) => renderMessage(message)).join("")}
    ${
      state.pendingReply
        ? renderMessage(
            {
              role: "assistant",
              content: state.streamingContent,
              createTime: new Date().toISOString(),
              ...state.streamMetadata,
            },
            true,
          )
        : ""
    }
  `;
}

function renderAgentAvatar(agent, size = 38) {
  if (agent?.avatar) {
    return `
      <span class="agent-avatar" style="width:${size}px;height:${size}px">
        <img src="${escapeHtml(agent.avatar)}" alt="" />
      </span>
    `;
  }
  return `
    <span class="agent-avatar" style="width:${size}px;height:${size}px">
      ${escapeHtml(initials(agent?.name || "A"))}
    </span>
  `;
}

function renderAgentQuickList() {
  if (state.workspaceBusy && !state.agents.length) {
    return `
      <div class="sidebar-empty">
        <span class="spinner small"></span>
        <div>正在加载 Agent</div>
      </div>
    `;
  }

  if (!state.agents.length) {
    return '<div class="sidebar-empty">暂无 Agent</div>';
  }

  return state.agents
    .map(
      (agent) => {
        const apiKey = apiKeyById(agent.apiKeyId);
        return `
        <button
          class="sidebar-agent-item ${agent.id === state.activeAgentId ? "active" : ""}"
          type="button"
          data-action="edit-agent"
          data-agent-id="${escapeHtml(agent.id)}"
        >
          ${renderAgentAvatar(agent, 30)}
          <span class="sidebar-agent-copy">
            <span class="sidebar-agent-name">${escapeHtml(agent.name || "未命名 Agent")}</span>
            <span class="sidebar-agent-meta">${escapeHtml(agent.modelId || apiKey?.name || "未配置模型")}</span>
          </span>
        </button>
      `;
      },
    )
    .join("");
}

function renderApiKeyQuickList() {
  if (state.workspaceBusy && !state.apiKeys.length) {
    return `
      <div class="sidebar-empty">
        <span class="spinner small"></span>
        <div>正在加载密钥</div>
      </div>
    `;
  }

  if (!state.apiKeys.length) {
    return '<div class="sidebar-empty">暂无 API 密钥</div>';
  }

  return state.apiKeys
    .map(
      (apiKey) => `
        <button
          class="sidebar-agent-item"
          type="button"
          data-action="edit-api-key"
          data-api-key-id="${escapeHtml(apiKey.id)}"
        >
          <span class="api-key-mark">${svgIcon("key", 15)}</span>
          <span class="sidebar-agent-copy">
            <span class="sidebar-agent-name">${escapeHtml(apiKeyDisplayName(apiKey))}</span>
            <span class="sidebar-agent-meta">
              ${escapeHtml(providerLabel(apiKey.provider))} · ${escapeHtml(apiKey.apiKey || "****")}
            </span>
          </span>
        </button>
      `,
    )
    .join("");
}

function renderApiKeyManager() {
  if (state.workspaceBusy && !state.apiKeys.length) {
    return `
      <section class="agent-page">
        <div class="agent-page-loading">
          <span class="spinner"></span>
          <span>正在加载 API 密钥</span>
        </div>
      </section>
    `;
  }

  if (!state.apiKeys.length) {
    return `
      <section class="agent-page">
        <div class="empty-state agent-empty-state">
          <div class="empty-icon">${svgIcon("key", 24)}</div>
          <h2>暂无 API 密钥</h2>
          <p>配置服务商密钥后，关联该服务商的 Agent 才能发起对话。</p>
          <button class="primary-button" type="button" data-action="new-api-key">
            ${svgIcon("plus", 16)}
            <span>新增 API 密钥</span>
          </button>
        </div>
      </section>
    `;
  }

  const enabledCount = state.apiKeys.filter((apiKey) => apiKey.enabled !== false).length;
  const disabledCount = state.apiKeys.length - enabledCount;

  return `
    <section class="agent-page">
      <div class="agent-page-toolbar">
        <div class="api-key-summary">
          <span class="provider-pill">${enabledCount} 个已启用</span>
          ${disabledCount ? `<span class="muted-text">${disabledCount} 个已停用</span>` : ""}
        </div>
        <span class="agent-count">${state.apiKeys.length} 个 API 密钥</span>
      </div>
      <div class="agent-table-shell">
        <div class="agent-table api-key-table agent-table-head" aria-hidden="true">
          <span>密钥</span>
          <span>API Key</span>
          <span>Base URL</span>
          <span>状态</span>
          <span>更新时间</span>
          <span></span>
        </div>
        <div class="agent-table-body">
          ${state.apiKeys
            .map((apiKey) => {
              const usageCount = apiKeyUsageCount(apiKey.id);
              return `
                <article class="agent-table api-key-table agent-table-row">
                  <div class="agent-identity">
                    <span class="api-key-mark large">${svgIcon("key", 17)}</span>
                    <div class="agent-identity-copy">
                      <strong>${escapeHtml(apiKeyDisplayName(apiKey))}</strong>
                      <span>
                        ${escapeHtml(providerLabel(apiKey.provider))} ·
                        ${usageCount ? `${usageCount} 个 Agent 使用` : "未关联 Agent"}
                      </span>
                    </div>
                  </div>
                  <div class="api-key-secret" data-label="API Key">
                    <code>${escapeHtml(apiKey.apiKey || "****")}</code>
                  </div>
                  <div class="api-key-endpoint" data-label="Base URL" title="${escapeHtml(apiKey.baseUrl || "")}">
                    ${escapeHtml(apiKey.baseUrl || "服务商默认地址")}
                  </div>
                  <div data-label="状态">
                    <span class="config-state ${apiKey.enabled === false ? "disabled" : "enabled"}">
                      ${apiKey.enabled === false ? "已停用" : "已启用"}
                    </span>
                  </div>
                  <div class="agent-time" data-label="更新时间">
                    ${escapeHtml(formatSessionTime(apiKey.updateTime || apiKey.createTime))}
                  </div>
                  <div class="agent-row-actions">
                    <button
                      class="icon-button"
                      type="button"
                      data-action="edit-api-key"
                      data-api-key-id="${escapeHtml(apiKey.id)}"
                      title="编辑"
                      aria-label="编辑 API 密钥"
                    >${svgIcon("pencil", 16)}</button>
                    <button
                      class="icon-button danger"
                      type="button"
                      data-action="delete-api-key"
                      data-api-key-id="${escapeHtml(apiKey.id)}"
                      title="删除"
                      aria-label="删除 API 密钥"
                    >${svgIcon("trash", 16)}</button>
                  </div>
                </article>
              `;
            })
            .join("")}
        </div>
      </div>
    </section>
  `;
}

function renderAgentManager() {
  if (state.workspaceBusy && !state.agents.length) {
    return `
      <section class="agent-page">
        <div class="agent-page-loading">
          <span class="spinner"></span>
          <span>正在加载 Agent</span>
        </div>
      </section>
    `;
  }

  if (!state.agents.length) {
    return `
      <section class="agent-page">
        <div class="empty-state agent-empty-state">
          <div class="empty-icon">${svgIcon("bot", 24)}</div>
          <h2>暂无 Agent</h2>
          <p>创建第一个 Agent 后，就可以为它建立会话并开始聊天。</p>
          <button class="primary-button" type="button" data-action="new-agent">
            ${svgIcon("plus", 16)}
            <span>新建 Agent</span>
          </button>
        </div>
      </section>
    `;
  }

  return `
    <section class="agent-page">
      <div class="agent-page-toolbar">
        <div class="agent-search">
          ${svgIcon("search", 17)}
          <label class="sr-only" for="agent-search">搜索 Agent</label>
          <input id="agent-search" type="search" placeholder="搜索名称、模型或服务商" autocomplete="off" />
        </div>
        <span class="agent-count">${state.agents.length} 个 Agent</span>
      </div>
      <div class="agent-table-shell">
        <div class="agent-table agent-table-head" aria-hidden="true">
          <span>Agent</span>
          <span>API 密钥</span>
          <span>模型</span>
          <span>工具</span>
          <span>更新时间</span>
          <span></span>
        </div>
        <div class="agent-table-body">
          ${state.agents
            .map((agent) => {
              const apiKey = apiKeyById(agent.apiKeyId);
              const searchText = [
                agent.name,
                agent.description,
                apiKey?.name,
                apiKey?.provider,
                agent.modelId,
                ...(agent.tools || []),
              ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();
              const tools = Array.isArray(agent.tools) ? agent.tools : [];

              return `
                <article
                  class="agent-table agent-table-row"
                  data-agent-row
                  data-search="${escapeHtml(searchText)}"
                >
                  <div class="agent-identity">
                    ${renderAgentAvatar(agent)}
                    <div class="agent-identity-copy">
                      <strong>${escapeHtml(agent.name || "未命名 Agent")}</strong>
                      <span>${escapeHtml(agent.description || "暂无描述")}</span>
                    </div>
                  </div>
                  <div class="agent-key-cell" data-label="API 密钥">
                    ${
                      apiKey
                        ? `
                          <span class="agent-key-copy">
                            <strong>${escapeHtml(apiKeyDisplayName(apiKey))}</strong>
                            <span>${escapeHtml(providerLabel(apiKey.provider))}</span>
                          </span>
                        `
                        : '<span class="muted-text">未配置</span>'
                    }
                  </div>
                  <div class="agent-model" data-label="模型">
                    ${escapeHtml(agent.modelId || "未配置")}
                  </div>
                  <div class="agent-tools" data-label="工具">
                    ${
                      tools.length
                        ? tools
                            .map((tool) => `<span class="tool-pill">${escapeHtml(tool)}</span>`)
                            .join("")
                        : '<span class="muted-text">无</span>'
                    }
                  </div>
                  <div class="agent-time" data-label="更新时间">
                    ${escapeHtml(formatSessionTime(agent.updateTime || agent.createTime))}
                  </div>
                  <div class="agent-row-actions">
                    <button
                      class="icon-button"
                      type="button"
                      data-action="chat-agent"
                      data-agent-id="${escapeHtml(agent.id)}"
                      title="开始对话"
                      aria-label="使用该 Agent 开始对话"
                    >${svgIcon("message", 16)}</button>
                    <button
                      class="icon-button"
                      type="button"
                      data-action="edit-agent"
                      data-agent-id="${escapeHtml(agent.id)}"
                      title="编辑"
                      aria-label="编辑 Agent"
                    >${svgIcon("pencil", 16)}</button>
                    <button
                      class="icon-button danger"
                      type="button"
                      data-action="delete-agent"
                      data-agent-id="${escapeHtml(agent.id)}"
                      title="删除"
                      aria-label="删除 Agent"
                    >${svgIcon("trash", 16)}</button>
                  </div>
                </article>
              `;
            })
            .join("")}
        </div>
      </div>
    </section>
  `;
}

function renderWorkspace() {
  const agent = activeAgent();
  const session = activeSession();
  const agentApiKey = agent ? apiKeyById(agent.apiKeyId) : null;
  const sessionTokenLocked = isSessionTokenExhausted(session);
  const username = state.auth?.username || "admin";

  app.innerHTML = `
    <div class="app-shell ${state.sidebarOpen ? "sidebar-open" : ""}">
      <aside class="sidebar" aria-label="会话侧边栏">
        <div class="sidebar-header">
          <div class="sidebar-brand">
            <span class="brand-mark">AX</span>
            <div class="sidebar-brand-text">
              <div class="sidebar-brand-name">AgentX</div>
              <div class="sidebar-brand-subtitle">${escapeHtml(agent?.name || "Workspace")}</div>
            </div>
          </div>
          <button
            class="icon-button close-sidebar-button"
            type="button"
            data-action="close-sidebar"
            title="关闭侧边栏"
            aria-label="关闭侧边栏"
          >${svgIcon("x", 18)}</button>
        </div>
        <nav class="sidebar-nav" aria-label="主导航">
          <button
            class="sidebar-nav-item ${state.view === "chat" ? "active" : ""}"
            type="button"
            data-action="show-chat"
          >
            ${svgIcon("message", 17)}
            <span>对话</span>
          </button>
          <button
            class="sidebar-nav-item ${state.view === "agents" ? "active" : ""}"
            type="button"
            data-action="show-agents"
          >
            ${svgIcon("bot", 17)}
            <span>Agent 管理</span>
          </button>
          <button
            class="sidebar-nav-item ${state.view === "apiKeys" ? "active" : ""}"
            type="button"
            data-action="show-api-keys"
          >
            ${svgIcon("key", 17)}
            <span>API 设置</span>
          </button>
        </nav>
        ${
          state.view === "chat"
            ? `
              <section class="sidebar-section">
                <div class="sidebar-section-header">
                  <span class="sidebar-section-title">会话</span>
                  <button
                    class="icon-button"
                    type="button"
                    data-action="new-session"
                    title="新建会话"
                    aria-label="新建会话"
                    ${!state.agents.length || state.workspaceBusy ? "disabled" : ""}
                  >${svgIcon("plus", 18)}</button>
                </div>
                <div class="session-list">${renderSessions()}</div>
              </section>
            `
            : state.view === "agents"
              ? `
              <section class="sidebar-section">
                <div class="sidebar-section-header">
                  <span class="sidebar-section-title">Agent</span>
                  <button
                    class="icon-button"
                    type="button"
                    data-action="new-agent"
                    title="新建 Agent"
                    aria-label="新建 Agent"
                  >${svgIcon("plus", 18)}</button>
                </div>
                <div class="session-list agent-quick-list">${renderAgentQuickList()}</div>
              </section>
            `
              : `
              <section class="sidebar-section">
                <div class="sidebar-section-header">
                  <span class="sidebar-section-title">API 密钥</span>
                  <button
                    class="icon-button"
                    type="button"
                    data-action="new-api-key"
                    title="新增 API 密钥"
                    aria-label="新增 API 密钥"
                  >${svgIcon("plus", 18)}</button>
                </div>
                <div class="session-list agent-quick-list">${renderApiKeyQuickList()}</div>
              </section>
            `
        }
        <div class="sidebar-footer">
          <div class="user-chip">
            <div class="user-avatar">${escapeHtml(initials(username))}</div>
            <div class="user-meta">
              <div class="user-name">${escapeHtml(username)}</div>
              <div class="user-state"><span class="status-dot"></span>已登录</div>
            </div>
          </div>
          <button
            class="icon-button"
            type="button"
            data-action="logout"
            title="退出登录"
            aria-label="退出登录"
          >${svgIcon("logOut", 18)}</button>
        </div>
      </aside>
      ${
        state.sidebarOpen
          ? '<button class="sidebar-backdrop" type="button" data-action="close-sidebar" aria-label="关闭侧边栏"></button>'
          : ""
      }
      <main class="workspace">
        <header class="workspace-header">
          <div class="workspace-header-main">
            <button
              class="icon-button mobile-menu-button"
              type="button"
              data-action="open-sidebar"
              title="打开侧边栏"
              aria-label="打开侧边栏"
            >${svgIcon("menu", 19)}</button>
            ${
              state.view === "chat"
                ? `
                  ${renderAgentPicker()}
                  <div class="workspace-title">
                    <h1>${escapeHtml(session?.title || agent?.name || "AgentX")}</h1>
                    <p>
                      <span>${escapeHtml(agent ? agent.modelId || agentApiKey?.name || "未配置模型" : "AI Workspace")}</span>
                      ${
                        session
                          ? `<span class="workspace-token-usage ${sessionTokenLocked ? "exhausted" : ""}">${escapeHtml(sessionTokenSummary(session))}</span>`
                          : ""
                      }
                    </p>
                  </div>
                `
                : state.view === "agents"
                  ? `
                  <div class="workspace-title always-visible">
                    <h1>Agent 管理</h1>
                    <p>${state.agents.length} 个 Agent</p>
                  </div>
                `
                  : `
                  <div class="workspace-title always-visible">
                    <h1>API 设置</h1>
                    <p>${state.apiKeys.length} 个 API 密钥</p>
                  </div>
                `
            }
          </div>
          <div class="header-actions">
            ${
              state.view === "agents"
                ? `
                  <button class="secondary-button header-create-button" type="button" data-action="new-agent">
                    ${svgIcon("plus", 16)}
                    <span>新建 Agent</span>
                  </button>
                `
                : state.view === "apiKeys"
                  ? `
                    <button class="secondary-button header-create-button" type="button" data-action="new-api-key">
                      ${svgIcon("plus", 16)}
                      <span>新增密钥</span>
                    </button>
                  `
                  : ""
            }
            <button
              class="icon-button"
              type="button"
              data-action="refresh"
              title="刷新数据"
              aria-label="刷新数据"
              ${state.workspaceBusy ? "disabled" : ""}
            >${svgIcon("refresh", 18)}</button>
            <span class="header-divider"></span>
            <button
              class="icon-button"
              type="button"
              data-action="logout"
              title="退出登录"
              aria-label="退出登录"
            >${svgIcon("logOut", 18)}</button>
          </div>
        </header>
        ${
          state.view === "chat"
            ? `
              <section class="chat-panel" aria-label="对话">
                <div class="message-viewport" id="message-viewport">
                  <div class="message-column">${renderChatContent()}</div>
                </div>
                <div class="composer-shell">
                  <form class="composer" id="composer-form">
                    <label class="sr-only" for="composer-input">消息</label>
                    <textarea
                      id="composer-input"
                      name="content"
                      rows="1"
                      maxlength="12000"
                      placeholder="${sessionTokenLocked ? "本会话 Token 额度已用完" : "输入消息"}"
                      ${!session || state.chatBusy || sessionTokenLocked ? "disabled" : ""}
                    ></textarea>
                    <div class="composer-footer">
                      <span class="composer-hint">${
                        session
                          ? escapeHtml(`${agent?.name || "Agent"} · ${sessionTokenSummary(session, true)}`)
                          : "请先创建会话"
                      }</span>
                      ${
                        state.chatBusy
                          ? `
                            <button
                              class="icon-button stop-button"
                              type="button"
                              data-action="stop-generation"
                              title="停止生成"
                              aria-label="停止生成"
                            >${svgIcon("stop", 15)}</button>
                          `
                          : `
                            <button
                              class="icon-button send-button"
                              type="submit"
                              title="发送"
                              aria-label="发送消息"
                              ${!session || sessionTokenLocked ? "disabled" : ""}
                            >${svgIcon("send", 17)}</button>
                          `
                      }
                    </div>
                  </form>
                </div>
              </section>
            `
            : state.view === "agents"
              ? renderAgentManager()
              : renderApiKeyManager()
        }
      </main>
    </div>
    ${renderModal()}
  `;

  window.requestAnimationFrame(scrollMessagesToBottom);
}

function renderAgentEditorModal() {
  const agent =
    state.modal?.agentId
      ? state.agents.find((item) => item.id === state.modal.agentId)
      : null;
  const editing = Boolean(agent);
  const values = {
    name: agent?.name || "",
    avatar: agent?.avatar || "",
    description: agent?.description || "",
    systemPrompt: agent?.systemPrompt || "",
    welcomeMessage: agent?.welcomeMessage || "",
    apiKeyId:
      agent?.apiKeyId ||
      state.apiKeys.find((apiKey) => apiKey.enabled !== false)?.id ||
      "",
    modelId: agent?.modelId || "",
  };
  const toolNames = Array.from(new Set(["weather", "file", ...(agent?.tools || [])]));
  const selectedTools = new Set(agent?.tools || []);
  const enabledApiKeys = state.apiKeys.filter((apiKey) => apiKey.enabled !== false);

  return `
    <div class="modal-backdrop agent-editor-backdrop" data-modal-backdrop>
      <section class="modal agent-editor-modal" role="dialog" aria-modal="true" aria-labelledby="agent-editor-title">
        <div class="modal-header">
          <div>
            <h2 class="modal-title" id="agent-editor-title">${editing ? "编辑 Agent" : "新建 Agent"}</h2>
            <p class="modal-subtitle">${escapeHtml(agent?.id || "配置模型、提示词和可用工具")}</p>
          </div>
          <button class="icon-button" type="button" data-action="close-modal" aria-label="关闭">
            ${svgIcon("x", 18)}
          </button>
        </div>
        <form id="agent-form">
          <div class="modal-body agent-editor-body">
            <section class="agent-form-section">
              <h3 class="agent-form-section-title">基础信息</h3>
              <div class="field-grid two-columns">
                <div class="field">
                  <label for="agent-name">名称</label>
                  <input id="agent-name" name="name" maxlength="100" value="${escapeHtml(values.name)}" required autofocus />
                </div>
                <div class="field">
                  <label for="agent-avatar">头像 URL</label>
                  <input id="agent-avatar" name="avatar" maxlength="500" value="${escapeHtml(values.avatar)}" />
                </div>
              </div>
              <div class="field">
                <label for="agent-description">描述</label>
                <textarea id="agent-description" name="description" maxlength="500">${escapeHtml(values.description)}</textarea>
              </div>
            </section>

            <section class="agent-form-section">
              <h3 class="agent-form-section-title">模型配置</h3>
              <div class="field-grid two-columns">
                <div class="field">
                  <label for="agent-api-key">API 密钥</label>
                  <select id="agent-api-key" name="apiKeyId" required ${enabledApiKeys.length ? "" : "disabled"}>
                    ${
                      enabledApiKeys.length
                        ? enabledApiKeys
                            .map(
                              (apiKey) => `
                                <option value="${escapeHtml(apiKey.id)}" ${apiKey.id === values.apiKeyId ? "selected" : ""}>
                                  ${escapeHtml(apiKeyDisplayName(apiKey))} · ${escapeHtml(providerLabel(apiKey.provider))}
                                </option>
                              `,
                            )
                            .join("")
                        : '<option value="">请先配置 API 密钥</option>'
                    }
                  </select>
                </div>
                <div class="field">
                  <label for="agent-model">模型 ID</label>
                  <input id="agent-model" name="modelId" maxlength="120" value="${escapeHtml(values.modelId)}" placeholder="gpt-4o-mini" required />
                </div>
              </div>
              ${
                enabledApiKeys.length
                  ? ""
                  : '<p class="form-hint">暂无启用中的 API 密钥，请先前往 API 设置添加。</p>'
              }
            </section>

            <section class="agent-form-section">
              <h3 class="agent-form-section-title">提示词</h3>
              <div class="field">
                <label for="agent-system-prompt">System Prompt</label>
                <textarea id="agent-system-prompt" name="systemPrompt" maxlength="8000">${escapeHtml(values.systemPrompt)}</textarea>
              </div>
              <div class="field">
                <label for="agent-welcome-message">欢迎消息</label>
                <textarea id="agent-welcome-message" name="welcomeMessage" maxlength="1000">${escapeHtml(values.welcomeMessage)}</textarea>
              </div>
            </section>

            <section class="agent-form-section">
              <h3 class="agent-form-section-title">工具</h3>
              <div class="tool-option-grid">
                ${toolNames
                  .map(
                    (tool) => `
                      <label class="tool-option">
                        <input type="checkbox" name="tools" value="${escapeHtml(tool)}" ${selectedTools.has(tool) ? "checked" : ""} />
                        <span>${escapeHtml(tool)}</span>
                      </label>
                    `,
                  )
                  .join("")}
              </div>
            </section>
          </div>
          <div class="modal-footer agent-editor-footer">
            <button class="ghost-button" type="button" data-action="close-modal">取消</button>
            <button class="primary-button" type="submit" ${state.agentBusy || !enabledApiKeys.length ? "disabled" : ""}>
              ${state.agentBusy ? '<span class="button-spinner"></span>' : svgIcon("save", 16)}
              <span>${editing ? "保存" : "创建"}</span>
            </button>
          </div>
        </form>
      </section>
    </div>
  `;
}

function renderDeleteAgentModal() {
  const agent = state.agents.find((item) => item.id === state.modal?.agentId);
  const sessionCount = state.sessions.filter((session) => session.agentId === agent?.id).length;

  return `
    <div class="modal-backdrop" data-modal-backdrop>
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="delete-agent-title">
        <div class="modal-header">
          <h2 class="modal-title" id="delete-agent-title">删除 Agent</h2>
          <button class="icon-button" type="button" data-action="close-modal" aria-label="关闭">
            ${svgIcon("x", 18)}
          </button>
        </div>
        <div class="modal-body">
          <div class="modal-danger-mark">${svgIcon("trash", 19)}</div>
          <p class="modal-copy">
            Agent“${escapeHtml(agent?.name || "未命名 Agent")}”将被永久删除。
            ${sessionCount ? `当前有 ${sessionCount} 个关联会话，删除 Agent 后这些会话将不可继续使用。` : ""}
          </p>
        </div>
        <div class="modal-footer">
          <button class="ghost-button" type="button" data-action="close-modal">取消</button>
          <button class="primary-button" type="button" data-action="confirm-delete-agent" ${state.agentBusy ? "disabled" : ""}>
            ${svgIcon("trash", 16)}
            <span>删除</span>
          </button>
        </div>
      </section>
    </div>
  `;
}

function renderApiKeyEditorModal() {
  const apiKey =
    state.modal?.apiKeyId
      ? state.apiKeys.find((item) => item.id === state.modal.apiKeyId)
      : null;
  const editing = Boolean(apiKey);
  const enabled = apiKey?.enabled !== false;

  return `
    <div class="modal-backdrop agent-editor-backdrop" data-modal-backdrop>
      <section class="modal agent-editor-modal api-key-editor-modal" role="dialog" aria-modal="true" aria-labelledby="api-key-editor-title">
        <div class="modal-header">
          <div>
            <h2 class="modal-title" id="api-key-editor-title">${editing ? "编辑 API 密钥" : "新增 API 密钥"}</h2>
            <p class="modal-subtitle">${escapeHtml(apiKey?.id || "配置接口协议、访问凭据和请求地址")}</p>
          </div>
          <button class="icon-button" type="button" data-action="close-modal" aria-label="关闭">
            ${svgIcon("x", 18)}
          </button>
        </div>
        <form id="api-key-form" autocomplete="off">
          <div class="modal-body agent-editor-body">
            <section class="agent-form-section">
              <h3 class="agent-form-section-title">密钥信息</h3>
              <div class="field">
                <label for="api-key-name">名称</label>
                <input
                  id="api-key-name"
                  name="name"
                  maxlength="128"
                  value="${escapeHtml(apiKey?.name || "")}"
                  placeholder="例如：DeepSeek 主账号"
                  required
                  autofocus
                />
              </div>
              <div class="field">
                <label for="api-key-provider">接口协议</label>
                <select id="api-key-provider" name="provider" required>
                  <option value="openai" ${apiKey?.provider === "openai" || !apiKey ? "selected" : ""}>OpenAI / OpenAI 兼容</option>
                  <option value="anthropic" ${apiKey?.provider === "anthropic" ? "selected" : ""}>Anthropic</option>
                </select>
              </div>
            </section>

            <section class="agent-form-section">
              <h3 class="agent-form-section-title">访问凭据</h3>
              <div class="field">
                <label for="api-key-secret">API Key</label>
                <input
                  id="api-key-secret"
                  name="apiKey"
                  type="password"
                  maxlength="1000"
                  placeholder="${editing ? `当前：${escapeHtml(apiKey?.apiKey || "****")}，留空则保留` : "请输入 API Key"}"
                  autocomplete="new-password"
                  ${editing ? "" : "required"}
                />
              </div>
              <div class="field">
                <label for="api-key-base-url">Base URL</label>
                <input
                  id="api-key-base-url"
                  name="baseUrl"
                  maxlength="500"
                  value="${escapeHtml(apiKey?.baseUrl || "")}"
                  placeholder="留空使用服务商默认地址"
                />
              </div>
              <label class="toggle-field">
                <input type="checkbox" name="enabled" ${enabled ? "checked" : ""} />
                <span><strong>启用该密钥</strong></span>
              </label>
            </section>
          </div>
          <div class="modal-footer agent-editor-footer">
            <button class="ghost-button" type="button" data-action="close-modal">取消</button>
            <button class="primary-button" type="submit" ${state.apiKeyBusy ? "disabled" : ""}>
              ${state.apiKeyBusy ? '<span class="button-spinner"></span>' : svgIcon("save", 16)}
              <span>${editing ? "保存" : "新增"}</span>
            </button>
          </div>
        </form>
      </section>
    </div>
  `;
}

function renderDeleteApiKeyModal() {
  const apiKey = state.apiKeys.find((item) => item.id === state.modal?.apiKeyId);
  const usageCount = apiKey ? apiKeyUsageCount(apiKey.id) : 0;

  return `
    <div class="modal-backdrop" data-modal-backdrop>
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="delete-api-key-title">
        <div class="modal-header">
          <h2 class="modal-title" id="delete-api-key-title">删除 API 密钥</h2>
          <button class="icon-button" type="button" data-action="close-modal" aria-label="关闭">
            ${svgIcon("x", 18)}
          </button>
        </div>
        <div class="modal-body">
          <div class="modal-danger-mark">${svgIcon("trash", 19)}</div>
          <p class="modal-copy">
            密钥“${escapeHtml(apiKeyDisplayName(apiKey))}”（${escapeHtml(apiKey?.apiKey || "****")}）将被永久删除。
            ${usageCount ? `当前有 ${usageCount} 个 Agent 正在使用，删除后这些 Agent 将无法发起对话。` : ""}
          </p>
        </div>
        <div class="modal-footer">
          <button class="ghost-button" type="button" data-action="close-modal">取消</button>
          <button class="primary-button" type="button" data-action="confirm-delete-api-key" ${state.apiKeyBusy ? "disabled" : ""}>
            ${svgIcon("trash", 16)}
            <span>删除</span>
          </button>
        </div>
      </section>
    </div>
  `;
}

function renderModal() {
  if (!state.modal) {
    return "";
  }

  if (state.modal.type === "api-key-editor") {
    return renderApiKeyEditorModal();
  }

  if (state.modal.type === "delete-api-key") {
    return renderDeleteApiKeyModal();
  }

  if (state.modal.type === "agent-editor") {
    return renderAgentEditorModal();
  }

  if (state.modal.type === "delete-agent") {
    return renderDeleteAgentModal();
  }

  if (state.modal.type === "create-session") {
    return `
      <div class="modal-backdrop" data-modal-backdrop>
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <div class="modal-header">
            <h2 class="modal-title" id="modal-title">新建会话</h2>
            <button class="icon-button" type="button" data-action="close-modal" aria-label="关闭">
              ${svgIcon("x", 18)}
            </button>
          </div>
          <form id="create-session-form">
            <div class="modal-body">
              <div class="field">
                <label for="new-session-agent">Agent</label>
                <select id="new-session-agent" name="agentId" required>
                  ${state.agents
                    .map(
                      (agent) => `
                        <option value="${escapeHtml(agent.id)}" ${agent.id === state.activeAgentId ? "selected" : ""}>
                          ${escapeHtml(agent.name || "未命名 Agent")}
                        </option>
                      `,
                    )
                    .join("")}
                </select>
              </div>
              <div class="field">
                <label for="new-session-title">标题</label>
                <input id="new-session-title" name="title" maxlength="128" placeholder="新会话" />
              </div>
              <div class="field">
                <label for="new-session-token-limit">会话 Token 上限</label>
                <input
                  id="new-session-token-limit"
                  name="tokenLimit"
                  type="number"
                  min="1"
                  step="1"
                  placeholder="留空则不限制"
                />
              </div>
            </div>
            <div class="modal-footer">
              <button class="ghost-button" type="button" data-action="close-modal">取消</button>
              <button class="primary-button" type="submit" ${state.workspaceBusy ? "disabled" : ""}>
                ${state.workspaceBusy ? '<span class="button-spinner"></span>' : svgIcon("plus", 16)}
                <span>创建</span>
              </button>
            </div>
          </form>
        </section>
      </div>
    `;
  }

  if (state.modal.type === "rename-session") {
    const session = state.sessions.find((item) => item.id === state.modal.sessionId);
    return `
      <div class="modal-backdrop" data-modal-backdrop>
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <div class="modal-header">
            <h2 class="modal-title" id="modal-title">重命名会话</h2>
            <button class="icon-button" type="button" data-action="close-modal" aria-label="关闭">
              ${svgIcon("x", 18)}
            </button>
          </div>
          <form id="rename-session-form">
            <div class="modal-body">
              <div class="field">
                <label for="rename-session-title">标题</label>
                <input
                  id="rename-session-title"
                  name="title"
                  maxlength="128"
                  value="${escapeHtml(session?.title || "")}"
                  required
                  autofocus
                />
              </div>
            </div>
            <div class="modal-footer">
              <button class="ghost-button" type="button" data-action="close-modal">取消</button>
              <button class="primary-button" type="submit" ${state.workspaceBusy ? "disabled" : ""}>
                <span>保存</span>
              </button>
            </div>
          </form>
        </section>
      </div>
    `;
  }

  if (state.modal.type === "delete-session") {
    const session = state.sessions.find((item) => item.id === state.modal.sessionId);
    return `
      <div class="modal-backdrop" data-modal-backdrop>
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <div class="modal-header">
            <h2 class="modal-title" id="modal-title">删除会话</h2>
            <button class="icon-button" type="button" data-action="close-modal" aria-label="关闭">
              ${svgIcon("x", 18)}
            </button>
          </div>
          <div class="modal-body">
            <div class="modal-danger-mark">${svgIcon("trash", 19)}</div>
            <p class="modal-copy">
              会话“${escapeHtml(session?.title || "新会话")}”及其中的消息将被永久删除。
            </p>
          </div>
          <div class="modal-footer">
            <button class="ghost-button" type="button" data-action="close-modal">取消</button>
            <button class="primary-button" type="button" data-action="confirm-delete-session" ${state.workspaceBusy ? "disabled" : ""}>
              ${svgIcon("trash", 16)}
              <span>删除</span>
            </button>
          </div>
        </section>
      </div>
    `;
  }

  return "";
}

function scrollMessagesToBottom({ behavior = "auto" } = {}) {
  const viewport = document.querySelector("#message-viewport");
  if (viewport) {
    viewport.scrollTo({
      top: viewport.scrollHeight,
      behavior,
    });
  }
}

let streamingRenderFrame = 0;

function renderStreamingReply() {
  streamingRenderFrame = 0;

  const bubble = document.querySelector(".message-row.assistant.pending .message-bubble");
  if (!bubble || !state.streamingContent) {
    return;
  }

  bubble.textContent = state.streamingContent;
  const cursor = document.createElement("span");
  cursor.className = "streaming-cursor";
  cursor.setAttribute("aria-hidden", "true");
  bubble.append(cursor);
  scrollMessagesToBottom();
}

function scheduleStreamingRender() {
  if (streamingRenderFrame) {
    return;
  }
  streamingRenderFrame = window.requestAnimationFrame(renderStreamingReply);
}

async function bootstrap() {
  if (!state.auth?.token) {
    renderLogin();
    return;
  }

  try {
    await refreshWorkspace({ render: false, quiet: true });
    renderWorkspace();
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
    }
    renderLogin(error.message);
  }
}

async function refreshWorkspace({ render = true, quiet = false } = {}) {
  state.workspaceBusy = true;
  if (render) {
    renderWorkspace();
  }

  try {
    const [agents, sessions, apiKeys] = await Promise.all([
      apiRequest("/agent/list"),
      apiRequest("/session/list"),
      apiRequest("/apikey/list"),
    ]);

    state.agents = Array.isArray(agents) ? agents : [];
    state.apiKeys = Array.isArray(apiKeys) ? apiKeys : [];
    const agentIds = new Set(state.agents.map((agent) => agent.id));
    state.sessions = (Array.isArray(sessions) ? sessions : []).filter((session) =>
      agentIds.has(session.agentId),
    );

    if (!state.agents.some((agent) => agent.id === state.activeAgentId)) {
      state.activeAgentId = state.agents[0]?.id || "";
    }

    if (!state.sessions.some((session) => session.id === state.activeSessionId)) {
      state.activeSessionId = "";
    }

    persistSelection();

    if (state.activeSessionId) {
      await loadMessages(state.activeSessionId, { render: false });
    } else {
      state.messages = [];
    }
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      throw error;
    }
    if (!quiet) {
      showToast("error", "刷新失败", error.message);
    }
    throw error;
  } finally {
    state.workspaceBusy = false;
    if (render && state.auth) {
      renderWorkspace();
    }
  }
}

async function loadMessages(sessionId, { render = true } = {}) {
  state.messagesBusy = true;
  if (render) {
    renderWorkspace();
  }

  try {
    const messages = await apiRequest(`/message/list?sessionId=${encodeURIComponent(sessionId)}`);
    if (state.activeSessionId === sessionId) {
      state.messages = Array.isArray(messages) ? messages : [];
    }
  } finally {
    state.messagesBusy = false;
    if (render && state.auth) {
      renderWorkspace();
    }
  }
}

async function selectSession(sessionId) {
  const session = state.sessions.find((item) => item.id === sessionId);
  if (!session) {
    return;
  }

  resetStreamingState();
  state.activeSessionId = session.id;
  state.activeAgentId = session.agentId || state.activeAgentId;
  state.view = "chat";
  state.messages = [];
  state.sidebarOpen = false;
  persistSelection();
  renderWorkspace();

  try {
    await loadMessages(session.id);
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", "消息加载失败", error.message);
  }
}

async function selectAgent(agentId) {
  if (!state.agents.some((agent) => agent.id === agentId)) {
    return;
  }

  resetStreamingState();
  state.activeAgentId = agentId;
  state.view = "chat";
  const latestSession = state.sessions.find((session) => session.agentId === agentId);
  state.activeSessionId = latestSession?.id || "";
  state.messages = [];
  persistSelection();
  renderWorkspace();

  if (latestSession) {
    try {
      await loadMessages(latestSession.id);
    } catch (error) {
      if (error.code === "401") {
        clearAuth();
        renderLogin(error.message);
        return;
      }
      showToast("error", "消息加载失败", error.message);
    }
  }
}

async function createSession(agentId, title, tokenLimit) {
  state.workspaceBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    const created = await apiRequest("/session/create", {
      method: "POST",
      body: {
        agentId,
        title: title.trim() || "新会话",
        tokenLimit,
      },
    });

    state.sessions = [created, ...state.sessions.filter((session) => session.id !== created.id)];
    state.activeAgentId = created.agentId;
    state.activeSessionId = created.id;
    state.view = "chat";
    state.messages = [];
    persistSelection();
    showToast("success", "会话已创建");
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", "创建失败", error.message);
  } finally {
    state.workspaceBusy = false;
    renderWorkspace();
  }
}

async function renameSession(sessionId, title) {
  state.workspaceBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    const updated = await apiRequest(
      `/session/rename?id=${encodeURIComponent(sessionId)}&title=${encodeURIComponent(title.trim())}`,
      { method: "POST" },
    );
    state.sessions = state.sessions.map((session) => (session.id === updated.id ? updated : session));
    showToast("success", "会话已重命名");
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", "重命名失败", error.message);
  } finally {
    state.workspaceBusy = false;
    renderWorkspace();
  }
}

async function deleteSession(sessionId) {
  state.workspaceBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    await apiRequest(`/session/delete?id=${encodeURIComponent(sessionId)}`, { method: "POST" });
    state.sessions = state.sessions.filter((session) => session.id !== sessionId);

    if (state.activeSessionId === sessionId) {
      const nextSession = state.sessions.find((session) => session.agentId === state.activeAgentId);
      state.activeSessionId = nextSession?.id || "";
      state.messages = [];
      if (nextSession) {
        await loadMessages(nextSession.id, { render: false });
      }
    }

    persistSelection();
    showToast("success", "会话已删除");
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", "删除失败", error.message);
  } finally {
    state.workspaceBusy = false;
    renderWorkspace();
  }
}

function optionalPositiveInteger(value) {
  const text = String(value ?? "").trim();
  if (!text) {
    return null;
  }
  const number = Number(text);
  if (!Number.isInteger(number) || number < 1) {
    throw new ApiError("Token 上限必须是大于 0 的整数");
  }
  return number;
}

async function saveAgent(form) {
  const formData = new FormData(form);
  const agentId = state.modal?.agentId || "";
  const editing = Boolean(agentId);
  let payload;

  try {
    payload = {
      name: String(formData.get("name") || "").trim(),
      avatar: String(formData.get("avatar") || "").trim() || null,
      description: String(formData.get("description") || "").trim() || null,
      systemPrompt: String(formData.get("systemPrompt") || "").trim() || null,
      welcomeMessage: String(formData.get("welcomeMessage") || "").trim() || null,
      apiKeyId: String(formData.get("apiKeyId") || "").trim(),
      modelId: String(formData.get("modelId") || "").trim(),
      tools: formData.getAll("tools").map(String),
    };
    if (!payload.apiKeyId) {
      throw new ApiError("请选择 API 密钥");
    }
  } catch (error) {
    showToast("error", "保存失败", error.message);
    return;
  }

  if (editing) {
    payload.id = agentId;
  }

  state.agentBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    const saved = await apiRequest(editing ? "/agent/update" : "/agent/create", {
      method: "POST",
      body: payload,
    });

    if (editing) {
      state.agents = state.agents.map((agent) => (agent.id === saved.id ? saved : agent));
    } else {
      state.agents = [saved, ...state.agents.filter((agent) => agent.id !== saved.id)];
      state.activeAgentId = saved.id;
    }

    persistSelection();
    showToast("success", editing ? "Agent 已更新" : "Agent 已创建");
    if (!apiKeyById(saved.apiKeyId)) {
      showToast(
        "info",
        "API 密钥未配置",
        "请前往 API 设置添加密钥后再发起对话。",
      );
    }
    await refreshWorkspace({ render: false, quiet: true });
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", editing ? "更新失败" : "创建失败", error.message);
  } finally {
    state.agentBusy = false;
    renderWorkspace();
  }
}

async function deleteAgent(agentId) {
  state.agentBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    await apiRequest(`/agent/delete?id=${encodeURIComponent(agentId)}`, { method: "POST" });
    state.agents = state.agents.filter((agent) => agent.id !== agentId);
    state.sessions = state.sessions.filter((session) => session.agentId !== agentId);

    if (state.activeAgentId === agentId) {
      state.activeAgentId = state.agents[0]?.id || "";
      state.activeSessionId = "";
      state.messages = [];
    }

    persistSelection();
    showToast("success", "Agent 已删除");
    await refreshWorkspace({ render: false, quiet: true });
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", "删除失败", error.message);
  } finally {
    state.agentBusy = false;
    renderWorkspace();
  }
}

async function saveApiKey(form) {
  const formData = new FormData(form);
  const apiKeyId = state.modal?.apiKeyId || "";
  const editing = Boolean(apiKeyId);
  const name = String(formData.get("name") || "").trim();
  const apiKey = String(formData.get("apiKey") || "").trim();

  if (!name) {
    showToast("error", "保存失败", "密钥名称不能为空");
    return;
  }
  if (!editing && !apiKey) {
    showToast("error", "保存失败", "API Key 不能为空");
    return;
  }

  const payload = {
    name,
    provider: String(formData.get("provider") || "").trim(),
    baseUrl: String(formData.get("baseUrl") || "").trim() || null,
    enabled: formData.get("enabled") === "on",
  };
  if (apiKey) {
    payload.apiKey = apiKey;
  }
  if (editing) {
    payload.id = apiKeyId;
  }

  state.apiKeyBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    const saved = await apiRequest(editing ? "/apikey/update" : "/apikey/create", {
      method: "POST",
      body: payload,
    });

    state.apiKeys = editing
      ? state.apiKeys.map((item) => (item.id === saved.id ? saved : item))
      : [saved, ...state.apiKeys.filter((item) => item.id !== saved.id)];
    showToast("success", editing ? "API 密钥已更新" : "API 密钥已新增");
    await refreshWorkspace({ render: false, quiet: true });
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", editing ? "更新失败" : "新增失败", error.message);
  } finally {
    state.apiKeyBusy = false;
    renderWorkspace();
  }
}

async function deleteApiKey(apiKeyId) {
  const apiKey = state.apiKeys.find((item) => item.id === apiKeyId);
  const usageCount = apiKey ? apiKeyUsageCount(apiKey.id) : 0;
  state.apiKeyBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    await apiRequest(`/apikey/delete?id=${encodeURIComponent(apiKeyId)}`, { method: "POST" });
    state.apiKeys = state.apiKeys.filter((item) => item.id !== apiKeyId);
    showToast("success", "API 密钥已删除");
    if (usageCount) {
      showToast("info", "关联 Agent 需要重新配置", `${usageCount} 个 Agent 当前绑定的密钥已不存在。`);
    }
  } catch (error) {
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    showToast("error", "删除失败", error.message);
  } finally {
    state.apiKeyBusy = false;
    renderWorkspace();
  }
}

function stopGeneration() {
  const controller = state.streamAbortController;
  if (!controller) {
    return;
  }

  const partialReply = state.streamingContent;
  const streamMetadata = { ...state.streamMetadata };
  const estimatedCompletionTokens = estimateTokenCount(partialReply);
  const estimatedTotalTokens =
    estimatedCompletionTokens + (streamMetadata.promptTokens || 0);
  const latencyMs = state.streamStartedAt
    ? Date.now() - state.streamStartedAt
    : streamMetadata.latencyMs;
  const sessionId = state.activeSessionId;
  controller.abort();
  state.streamAbortController = null;
  state.chatBusy = false;
  state.pendingReply = false;
  state.streamingContent = "";
  state.streamMetadata = createEmptyStreamMetadata();
  state.streamStartedAt = 0;

  if (partialReply.trim()) {
    state.messages = [
      ...state.messages,
      {
        id: streamMetadata.messageId || `stopped-${Date.now()}`,
        role: "assistant",
        content: partialReply,
        tokens: estimatedTotalTokens,
        promptTokens: streamMetadata.promptTokens,
        completionTokens: estimatedCompletionTokens,
        totalTokens: estimatedTotalTokens,
        model: streamMetadata.model,
        provider: streamMetadata.provider,
        finishReason: "cancelled",
        latencyMs,
        usageSource: "estimated",
        createTime: new Date().toISOString(),
      },
    ];
  }

  renderWorkspace();
  showToast("info", "已停止生成");

  window.setTimeout(async () => {
    if (state.chatBusy || state.activeSessionId !== sessionId) {
      return;
    }
    try {
      await loadMessages(sessionId, { render: false });
      renderWorkspace();
    } catch {
      // 后端可能仍在保存部分回复，保留前端已经显示的内容即可。
    }
  }, 1000);
}

async function sendMessage(content) {
  const session = activeSession();
  const text = content.trim();
  if (!session || !text || state.chatBusy) {
    return;
  }
  if (isSessionTokenExhausted(session)) {
    showToast("info", "Token 额度已用完", "该会话不能再发送新消息。");
    return;
  }

  const optimisticMessage = {
    id: `pending-${Date.now()}`,
    role: "user",
    content: text,
    createTime: new Date().toISOString(),
  };

  state.messages = [...state.messages, optimisticMessage];
  state.chatBusy = true;
  state.pendingReply = true;
  state.streamingContent = "";
  state.streamMetadata = createEmptyStreamMetadata();
  state.streamStartedAt = Date.now();
  const streamController = new AbortController();
  state.streamAbortController = streamController;
  renderWorkspace();

  try {
    await streamApiRequest(
      "/conversation/stream",
      {
        sessionId: session.id,
        content: text,
      },
      (event) => {
        if (state.streamAbortController !== streamController || state.activeSessionId !== session.id) {
          return;
        }
        if (event.event === "start") {
          const payload = parseStreamPayload(event.data);
          if (payload) {
            applySessionTokenBudget(payload);
            state.streamMetadata = {
              ...state.streamMetadata,
              messageId: payload.messageId || "",
              model: payload.model || "",
              provider: payload.provider || "",
            };
          }
          return;
        }
        if (event.event === "usage") {
          const payload = parseStreamPayload(event.data);
          if (payload) {
            state.streamMetadata = {
              ...state.streamMetadata,
              promptTokens: payload.promptTokens,
              completionTokens: payload.completionTokens,
              totalTokens: payload.totalTokens,
              usageSource: payload.usageSource || "",
            };
          }
          return;
        }
        if (event.event === "done") {
          const payload = parseStreamPayload(event.data);
          if (payload) {
            applySessionTokenBudget(payload);
            state.streamMetadata = {
              ...state.streamMetadata,
              messageId: payload.messageId || state.streamMetadata.messageId,
              promptTokens: payload.promptTokens,
              completionTokens: payload.completionTokens,
              totalTokens: payload.totalTokens,
              usageSource: payload.usageSource || state.streamMetadata.usageSource,
              finishReason: payload.finishReason || "",
              latencyMs: payload.latencyMs,
            };
            if (payload.limitReached) {
              showToast("info", "Token 额度已用完", "本次回答已完成，该会话后续消息将无法发送。");
            }
          }
          return;
        }
        if (event.event !== "delta") {
          return;
        }
        state.streamingContent += event.data;
        scheduleStreamingRender();
      },
      streamController.signal,
    );

    await loadMessages(session.id, { render: false });
    await refreshWorkspace({ render: false, quiet: true });
  } catch (error) {
    if (error.name === "AbortError") {
      return;
    }
    if (error.code === "401") {
      clearAuth();
      renderLogin(error.message);
      return;
    }
    if (error.code === "429") {
      showToast("info", "Token 额度已用完", error.message);
    } else {
      showToast("error", "发送失败", error.message);
    }
    try {
      await loadMessages(session.id, { render: false });
    } catch {
      state.messages = state.messages.filter((message) => message.id !== optimisticMessage.id);
    }
  } finally {
    if (state.streamAbortController === streamController) {
      state.streamAbortController = null;
      state.chatBusy = false;
      state.pendingReply = false;
      state.streamingContent = "";
      state.streamMetadata = createEmptyStreamMetadata();
      state.streamStartedAt = 0;
      renderWorkspace();
    }
  }
}

async function handleLogin(form) {
  const formData = new FormData(form);
  const username = String(formData.get("username") || "").trim();
  const password = String(formData.get("password") || "");

  if (!username || !password) {
    renderLogin("用户名和密码不能为空");
    return;
  }

  state.loginBusy = true;
  renderLogin();

  try {
    const login = await apiRequest("/auth/login", {
      method: "POST",
      body: { username, password },
      authenticated: false,
    });

    state.auth = {
      userId: login.userId,
      username: login.username,
      token: login.token,
      expiresAt: login.expiresAt,
    };
    localStorage.setItem(AUTH_KEY, JSON.stringify(state.auth));
    state.loginBusy = false;
    await refreshWorkspace({ render: false, quiet: true });
    renderWorkspace();
  } catch (error) {
    state.loginBusy = false;
    renderLogin(error.message);
  }
}

function switchView(view) {
  state.view = view === "agents" || view === "apiKeys" ? view : "chat";
  state.sidebarOpen = false;
  renderWorkspace();
}

function openAgentEditor(agentId = "") {
  state.modal = {
    type: "agent-editor",
    agentId: agentId || "",
  };
  renderWorkspace();
}

function openDeleteAgentModal(agentId) {
  state.modal = {
    type: "delete-agent",
    agentId,
  };
  renderWorkspace();
}

function openApiKeyEditor(apiKeyId = "") {
  state.modal = {
    type: "api-key-editor",
    apiKeyId: apiKeyId || "",
  };
  renderWorkspace();
}

function openDeleteApiKeyModal(apiKeyId) {
  state.modal = {
    type: "delete-api-key",
    apiKeyId,
  };
  renderWorkspace();
}

async function chatWithAgent(agentId) {
  await selectAgent(agentId);
}

function openCreateSessionModal() {
  if (!state.agents.length) {
    showToast("info", "暂无可用 Agent");
    return;
  }
  state.view = "chat";
  state.modal = { type: "create-session" };
  renderWorkspace();
}

function openRenameSessionModal(sessionId) {
  state.modal = { type: "rename-session", sessionId };
  renderWorkspace();
}

function openDeleteSessionModal(sessionId) {
  state.modal = { type: "delete-session", sessionId };
  renderWorkspace();
}

function closeModal() {
  state.modal = null;
  renderWorkspace();
}

function logout() {
  clearAuth();
  renderLogin();
}

function resizeComposer(textarea) {
  textarea.style.height = "auto";
  textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
}

app.addEventListener("submit", async (event) => {
  const form = event.target;

  if (form.id === "login-form") {
    event.preventDefault();
    await handleLogin(form);
    return;
  }

  if (form.id === "create-session-form") {
    event.preventDefault();
    const formData = new FormData(form);
    try {
      await createSession(
        String(formData.get("agentId") || ""),
        String(formData.get("title") || ""),
        optionalPositiveInteger(formData.get("tokenLimit")),
      );
    } catch (error) {
      showToast("error", "创建失败", error.message);
    }
    return;
  }

  if (form.id === "rename-session-form") {
    event.preventDefault();
    const sessionId = state.modal?.sessionId;
    const formData = new FormData(form);
    if (sessionId) {
      await renameSession(sessionId, String(formData.get("title") || ""));
    }
    return;
  }

  if (form.id === "agent-form") {
    event.preventDefault();
    await saveAgent(form);
    return;
  }

  if (form.id === "api-key-form") {
    event.preventDefault();
    await saveApiKey(form);
    return;
  }

  if (form.id === "composer-form") {
    event.preventDefault();
    const formData = new FormData(form);
    await sendMessage(String(formData.get("content") || ""));
  }
});

app.addEventListener("click", async (event) => {
  if (event.target.matches("[data-modal-backdrop]")) {
    closeModal();
    return;
  }

  const button = event.target.closest("[data-action]");
  if (!button) {
    return;
  }

  const action = button.dataset.action;
  const sessionId = button.dataset.sessionId;
  const agentId = button.dataset.agentId;
  const apiKeyId = button.dataset.apiKeyId;

  if (action === "select-session") {
    await selectSession(sessionId);
  } else if (action === "new-session") {
    openCreateSessionModal();
  } else if (action === "rename-session") {
    openRenameSessionModal(sessionId);
  } else if (action === "delete-session") {
    openDeleteSessionModal(sessionId);
  } else if (action === "confirm-delete-session") {
    await deleteSession(sessionId || state.modal?.sessionId);
  } else if (action === "show-chat") {
    switchView("chat");
  } else if (action === "show-agents") {
    switchView("agents");
  } else if (action === "show-api-keys") {
    switchView("apiKeys");
  } else if (action === "new-agent") {
    openAgentEditor();
  } else if (action === "edit-agent") {
    openAgentEditor(agentId);
  } else if (action === "delete-agent") {
    openDeleteAgentModal(agentId);
  } else if (action === "confirm-delete-agent") {
    await deleteAgent(agentId || state.modal?.agentId);
  } else if (action === "chat-agent") {
    await chatWithAgent(agentId);
  } else if (action === "new-api-key") {
    openApiKeyEditor();
  } else if (action === "edit-api-key") {
    openApiKeyEditor(apiKeyId);
  } else if (action === "delete-api-key") {
    openDeleteApiKeyModal(apiKeyId);
  } else if (action === "confirm-delete-api-key") {
    await deleteApiKey(apiKeyId || state.modal?.apiKeyId);
  } else if (action === "stop-generation") {
    stopGeneration();
  } else if (action === "close-modal") {
    closeModal();
  } else if (action === "logout") {
    logout();
  } else if (action === "open-sidebar") {
    state.sidebarOpen = true;
    renderWorkspace();
  } else if (action === "close-sidebar") {
    state.sidebarOpen = false;
    renderWorkspace();
  } else if (action === "refresh") {
    try {
      await refreshWorkspace();
      showToast("success", "数据已刷新");
    } catch {
      // refreshWorkspace already reports actionable errors
    }
  }
});

app.addEventListener("change", async (event) => {
  if (event.target.matches("[data-agent-select]")) {
    await selectAgent(event.target.value);
  }
});

app.addEventListener("input", (event) => {
  if (event.target.id === "composer-input") {
    resizeComposer(event.target);
    return;
  }

  if (event.target.id === "agent-search") {
    const query = event.target.value.trim().toLowerCase();
    document.querySelectorAll("[data-agent-row]").forEach((row) => {
      row.hidden = query && !String(row.dataset.search || "").includes(query);
    });
  }
});

app.addEventListener("keydown", async (event) => {
  if (
    event.target.id === "composer-input" &&
    event.key === "Enter" &&
    !event.shiftKey
  ) {
    event.preventDefault();
    const content = event.target.value;
    await sendMessage(content);
    return;
  }

  if (
    event.target.matches('[data-action="select-session"]') &&
    (event.key === "Enter" || event.key === " ")
  ) {
    event.preventDefault();
    await selectSession(event.target.dataset.sessionId);
  }
});

bootstrap();
