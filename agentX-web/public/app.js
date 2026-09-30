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
  sessions: [],
  messages: [],
  activeAgentId: sessionStorage.getItem(AGENT_KEY) || "",
  activeSessionId: sessionStorage.getItem(SESSION_KEY) || "",
  view: "chat",
  modal: null,
  loginBusy: false,
  workspaceBusy: false,
  agentBusy: false,
  messagesBusy: false,
  chatBusy: false,
  sidebarOpen: false,
  pendingReply: false,
  streamingContent: "",
  streamAbortController: null,
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

function clearAuth() {
  resetStreamingState();
  localStorage.removeItem(AUTH_KEY);
  sessionStorage.removeItem(AGENT_KEY);
  sessionStorage.removeItem(SESSION_KEY);
  state.auth = null;
  state.agents = [];
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
          <div class="session-time">${escapeHtml(formatSessionTime(session.updateTime || session.createTime))}</div>
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
      </div>
    </article>
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
      (agent) => `
        <button
          class="sidebar-agent-item ${agent.id === state.activeAgentId ? "active" : ""}"
          type="button"
          data-action="edit-agent"
          data-agent-id="${escapeHtml(agent.id)}"
        >
          ${renderAgentAvatar(agent, 30)}
          <span class="sidebar-agent-copy">
            <span class="sidebar-agent-name">${escapeHtml(agent.name || "未命名 Agent")}</span>
            <span class="sidebar-agent-meta">${escapeHtml(agent.modelId || agent.provider || "未配置模型")}</span>
          </span>
        </button>
      `,
    )
    .join("");
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
          <span>服务商</span>
          <span>模型</span>
          <span>工具</span>
          <span>更新时间</span>
          <span></span>
        </div>
        <div class="agent-table-body">
          ${state.agents
            .map((agent) => {
              const searchText = [
                agent.name,
                agent.description,
                agent.provider,
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
                  <div data-label="服务商">
                    <span class="provider-pill">${escapeHtml(agent.provider || "未配置")}</span>
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
            : `
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
                    <p>${escapeHtml(agent?.modelId || agent?.provider || "AI Workspace")}</p>
                  </div>
                `
                : `
                  <div class="workspace-title always-visible">
                    <h1>Agent 管理</h1>
                    <p>${state.agents.length} 个 Agent</p>
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
                      placeholder="输入消息"
                      ${!session || state.chatBusy ? "disabled" : ""}
                    ></textarea>
                    <div class="composer-footer">
                      <span class="composer-hint">${
                        session
                          ? escapeHtml(agent?.name || "Agent")
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
                              ${!session ? "disabled" : ""}
                            >${svgIcon("send", 17)}</button>
                          `
                      }
                    </div>
                  </form>
                </div>
              </section>
            `
            : renderAgentManager()
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
    provider: agent?.provider || "openai",
    modelId: agent?.modelId || "",
    temperature: agent?.temperature ?? 0.7,
    topP: agent?.topP ?? 0.7,
    topK: agent?.topK ?? 50,
    maxTokens: agent?.maxTokens ?? "",
  };
  const toolNames = Array.from(new Set(["weather", "file", ...(agent?.tools || [])]));
  const selectedTools = new Set(agent?.tools || []);

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
                  <label for="agent-provider">服务商</label>
                  <select id="agent-provider" name="provider" required>
                    <option value="openai" ${values.provider === "openai" ? "selected" : ""}>OpenAI</option>
                    <option value="anthropic" ${values.provider === "anthropic" ? "selected" : ""}>Anthropic</option>
                  </select>
                </div>
                <div class="field">
                  <label for="agent-model">模型 ID</label>
                  <input id="agent-model" name="modelId" maxlength="120" value="${escapeHtml(values.modelId)}" placeholder="gpt-4o-mini" required />
                </div>
              </div>
              <div class="field-grid four-columns">
                <div class="field">
                  <label for="agent-temperature">Temperature</label>
                  <input id="agent-temperature" name="temperature" type="number" min="0" max="2" step="0.1" value="${escapeHtml(values.temperature)}" />
                </div>
                <div class="field">
                  <label for="agent-top-p">Top P</label>
                  <input id="agent-top-p" name="topP" type="number" min="0" max="1" step="0.1" value="${escapeHtml(values.topP)}" />
                </div>
                <div class="field">
                  <label for="agent-top-k">Top K</label>
                  <input id="agent-top-k" name="topK" type="number" min="0" step="1" value="${escapeHtml(values.topK)}" />
                </div>
                <div class="field">
                  <label for="agent-max-tokens">Max Tokens</label>
                  <input id="agent-max-tokens" name="maxTokens" type="number" min="1" step="1" value="${escapeHtml(values.maxTokens)}" />
                </div>
              </div>
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
            <button class="primary-button" type="submit" ${state.agentBusy ? "disabled" : ""}>
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

function renderModal() {
  if (!state.modal) {
    return "";
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
    const [agents, sessions] = await Promise.all([
      apiRequest("/agent/list"),
      apiRequest("/session/list"),
    ]);

    state.agents = Array.isArray(agents) ? agents : [];
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

async function createSession(agentId, title) {
  state.workspaceBusy = true;
  state.modal = null;
  renderWorkspace();

  try {
    const created = await apiRequest("/session/create", {
      method: "POST",
      body: {
        agentId,
        title: title.trim() || "新会话",
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

function optionalNumber(value) {
  const text = String(value ?? "").trim();
  if (!text) {
    return null;
  }
  const number = Number(text);
  if (!Number.isFinite(number)) {
    throw new ApiError("数值参数格式不正确");
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
      provider: String(formData.get("provider") || "").trim(),
      modelId: String(formData.get("modelId") || "").trim(),
      temperature: optionalNumber(formData.get("temperature")),
      topP: optionalNumber(formData.get("topP")),
      topK: optionalNumber(formData.get("topK")),
      maxTokens: optionalNumber(formData.get("maxTokens")),
      tools: formData.getAll("tools").map(String),
    };
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

function stopGeneration() {
  const controller = state.streamAbortController;
  if (!controller) {
    return;
  }

  const partialReply = state.streamingContent;
  controller.abort();
  state.streamAbortController = null;
  state.chatBusy = false;
  state.pendingReply = false;
  state.streamingContent = "";

  if (partialReply.trim()) {
    state.messages = [
      ...state.messages,
      {
        id: `stopped-${Date.now()}`,
        role: "assistant",
        content: partialReply,
        tokens: 0,
        createTime: new Date().toISOString(),
      },
    ];
  }

  renderWorkspace();
  showToast("info", "已停止生成");
}

async function sendMessage(content) {
  const session = activeSession();
  const text = content.trim();
  if (!session || !text || state.chatBusy) {
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
        if (
          state.streamAbortController !== streamController ||
          state.activeSessionId !== session.id ||
          event.event !== "delta"
        ) {
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
    showToast("error", "发送失败", error.message);
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
  state.view = view === "agents" ? "agents" : "chat";
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
    await createSession(String(formData.get("agentId") || ""), String(formData.get("title") || ""));
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
