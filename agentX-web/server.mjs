import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer, request as httpRequest } from "node:http";
import { request as httpsRequest } from "node:https";
import { extname, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = fileURLToPath(new URL(".", import.meta.url));
const publicDir = resolve(rootDir, "public");
const port = Number(process.env.PORT || 5173);
const backendUrl = new URL(process.env.BACKEND_URL || "http://127.0.0.1:8091");

const mimeTypes = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".ico": "image/x-icon",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
};

function sendJson(response, status, payload) {
  response.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  response.end(JSON.stringify(payload));
}

function proxyRequest(request, response) {
  const incomingUrl = new URL(request.url, `http://${request.headers.host || "localhost"}`);
  const targetPath = `${incomingUrl.pathname.slice(4)}${incomingUrl.search}` || "/";
  const headers = { ...request.headers, host: backendUrl.host };

  delete headers["content-length"];

  let proxyResponse = null;
  const proxyRequest = (backendUrl.protocol === "https:" ? httpsRequest : httpRequest)(
    {
      hostname: backendUrl.hostname,
      port: backendUrl.port || (backendUrl.protocol === "https:" ? 443 : 80),
      path: targetPath,
      method: request.method,
      headers,
    },
    (upstreamResponse) => {
      proxyResponse = upstreamResponse;
      response.writeHead(upstreamResponse.statusCode || 502, upstreamResponse.headers);
      upstreamResponse.pipe(response);
    },
  );

  const abortUpstream = () => {
    if (!proxyRequest.destroyed) {
      proxyRequest.destroy();
    }
    if (proxyResponse && !proxyResponse.destroyed) {
      proxyResponse.destroy();
    }
  };

  proxyRequest.on("error", () => {
    if (response.destroyed) {
      return;
    }
    if (!response.headersSent) {
      sendJson(response, 502, {
        code: "502",
        info: "后端服务不可用，请确认 8091 端口已经启动",
        data: null,
      });
    } else {
      response.end();
    }
  });

  // 浏览器点击“停止生成”会关闭当前响应，继续关闭上游请求，
  // 让 Spring 收到 CANCEL 并保存已经生成的部分内容。
  request.on("aborted", abortUpstream);
  response.on("close", () => {
    if (!response.writableFinished) {
      abortUpstream();
    }
  });

  request.pipe(proxyRequest);
}

function serveStatic(request, response) {
  const incomingUrl = new URL(request.url, `http://${request.headers.host || "localhost"}`);
  let pathname;

  try {
    pathname = decodeURIComponent(incomingUrl.pathname);
  } catch {
    sendJson(response, 400, { code: "400", info: "非法请求路径", data: null });
    return;
  }

  const relativePath = pathname === "/" ? "index.html" : pathname.replace(/^\/+/, "");
  let filePath = resolve(publicDir, relativePath);

  if (!filePath.startsWith(`${publicDir}${sep}`) && filePath !== publicDir) {
    response.writeHead(403);
    response.end("Forbidden");
    return;
  }

  if (!existsSync(filePath) || !statSync(filePath).isFile()) {
    filePath = resolve(publicDir, "index.html");
  }

  response.writeHead(200, {
    "Cache-Control": "no-store",
    "Content-Type": mimeTypes[extname(filePath)] || "application/octet-stream",
  });
  createReadStream(filePath).pipe(response);
}

const server = createServer((request, response) => {
  if (request.url?.startsWith("/api/")) {
    proxyRequest(request, response);
    return;
  }
  serveStatic(request, response);
});

server.listen(port, "127.0.0.1", () => {
  console.log(`AgentX Web: http://127.0.0.1:${port}`);
  console.log(`Backend proxy: ${backendUrl.origin}`);
});
