# studio-service

KnowNote Studio 的沙盒管理服务，基于 Node.js 20 + Fastify，负责 Docker 容器生命周期管理、工作区文件读写，以及将容器内的 Vite 开发服务器反代给前端。

## 功能概述

- 为每个 Studio 项目启动一个独立的 Docker 沙盒容器（基于 `knownote-studio-sandbox` 镜像）
- 提供工作区文件的读写 API（供 AI 代码生成写入文件）
- 通过 HTTP 反代把容器内 Vite 服务暴露给前端（`/preview/:projectId`）
- 通过 WebSocket 反代把容器内 Vite HMR 暴露给前端（`/ws/:projectId`）

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PORT` | `3001` | 服务监听端口 |
| `DOCKER_SOCKET` | `/var/run/docker.sock` | Docker Unix socket 路径 |
| `WORKSPACE_BASE` | `/tmp/knownote-studio` | 宿主机工作区根目录（会挂载进容器） |
| `SANDBOX_IMAGE` | `knownote-studio-sandbox:latest` | 沙盒容器镜像名称 |
| `HOST_NAME` | `localhost` | 宿主机名称（用于反代地址） |

> **macOS + colima 用户**：`DOCKER_SOCKET` 设为 `~/.colima/default/docker.sock`；`WORKSPACE_BASE` 必须在 `$HOME` 下（colima 默认只挂载用户目录）。

## 快速启动

```bash
npm install

# 开发模式（热重载）
npm run dev

# 生产模式
npm run build
npm start
```

健康检查：`curl http://localhost:3001/health` → `{"ok":true}`

## API 参考

### 健康检查

**`GET /health`**

```json
{ "ok": true }
```

---

### 容器管理 `/containers`

#### `POST /containers/:projectId/start`

启动（或复用）该项目的沙盒容器。若容器已运行则直接返回状态。

**路径参数**

| 参数 | 说明 |
|------|------|
| `projectId` | Studio 项目 ID |

**响应 200**

```json
{
  "status": "running",
  "vitePort": 54321,
  "apiPort": 54322
}
```

**响应 500**

```json
{ "error": "Cannot connect to Docker socket" }
```

---

#### `DELETE /containers/:projectId/stop`

停止并删除该项目的沙盒容器及工作区。

**路径参数**

| 参数 | 说明 |
|------|------|
| `projectId` | Studio 项目 ID |

**响应 200**

```json
{ "status": "stopped" }
```

---

#### `GET /containers/:projectId/status`

获取容器当前状态（不启动容器）。

**路径参数**

| 参数 | 说明 |
|------|------|
| `projectId` | Studio 项目 ID |

**响应 200（运行中）**

```json
{
  "status": "running",
  "vitePort": 54321,
  "apiPort": 54322
}
```

**响应 200（未启动）**

```json
{ "status": "stopped" }
```

---

### 文件管理 `/files`

工作区根目录为 `{WORKSPACE_BASE}/{projectId}/`。

#### `GET /files/:projectId`

列出工作区内所有文件的相对路径。

**路径参数**

| 参数 | 说明 |
|------|------|
| `projectId` | Studio 项目 ID |

**响应 200**

```json
{
  "files": [
    "src/App.tsx",
    "src/main.tsx",
    "index.html",
    "package.json",
    "vite.config.ts"
  ]
}
```

---

#### `GET /files/:projectId/*`

读取工作区内指定文件的内容，返回原始文本。

**路径参数**

| 参数 | 说明 |
|------|------|
| `projectId` | Studio 项目 ID |
| `*` | 文件相对路径，如 `src/App.tsx` |

**响应 200** — `Content-Type: text/plain; charset=utf-8`，文件原始内容。

**响应 404**

```json
{ "error": "File not found", "path": "src/App.tsx" }
```

---

#### `PUT /files/:projectId/*`

写入（创建或覆盖）工作区内指定文件。

**路径参数**

| 参数 | 说明 |
|------|------|
| `projectId` | Studio 项目 ID |
| `*` | 文件相对路径，如 `src/App.tsx` |

**请求体**（JSON 或纯文本均可）

```json
{ "content": "// file content here" }
```

或直接发送纯文本：

```
Content-Type: text/plain

// file content here
```

**响应 200**

```json
{ "ok": true }
```

**响应 400**

```json
{ "error": "..." }
```

---

### 预览反代

#### `GET /preview/:projectId`（及所有子路径）

HTTP 反代，将请求转发至容器内运行的 Vite 开发服务器。前端 `<iframe>` 直接加载此地址即可预览沙盒应用。

若容器未启动，返回 `502 Bad Gateway`。

---

### WebSocket 反代

#### `GET /ws/:projectId`（WebSocket Upgrade）

WebSocket 反代，将连接转发至容器内 Vite HMR WebSocket，支持热模块替换。

---

## 目录结构

```
studio-service/
├── src/
│   ├── index.ts              # Fastify 应用入口
│   ├── config.ts             # 环境变量读取
│   ├── ContainerManager.ts   # Docker 容器生命周期（启动/停止/状态）
│   ├── FileService.ts        # 工作区文件读写
│   ├── routes/
│   │   ├── containers.ts     # 容器管理路由
│   │   ├── files.ts          # 文件管理路由
│   │   ├── previewProxy.ts   # HTTP 反代路由
│   │   ├── proxy.ts          # WebSocket 反代（ws upgrade）
│   │   └── containerRegistry.ts  # 容器端口注册表
│   └── ws/
│       └── containerWs.ts    # WebSocket 路由（备用）
├── .env                      # 本地环境变量（不提交）
├── package.json
└── tsconfig.json
```

## 沙盒镜像

沙盒镜像位于项目根目录 `sandbox-image/`，基于 Node.js 20 Alpine，内含：
- React + Vite 项目模板（`workspace-template/`）
- 启动脚本：复制模板到工作区、安装依赖、启动 `vite --host 0.0.0.0`

构建命令（在项目根目录执行）：

```bash
cd sandbox-image
docker build -t knownote-studio-sandbox:latest .
```
