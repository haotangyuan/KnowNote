# 开发文档

本文档面向开发者，涵盖本地环境搭建、每个服务的启动方式、环境变量说明，以及完整 API 参考。

---

## 目录

- [环境要求](#环境要求)
- [初始化步骤](#初始化步骤)
  - [1. 克隆项目](#1-克隆项目)
  - [2. 启动 Docker 中间件](#2-启动-docker-中间件)
  - [3. 创建数据库](#3-创建数据库)
  - [4. 配置环境变量](#4-配置环境变量)
- [启动服务](#启动服务)
  - [Spring Boot 后端](#spring-boot-后端)
  - [Studio Service（Node.js）](#studio-servicenodejs)
  - [Studio 前端（React）](#studio-前端react)
- [功能启用状态](#功能启用状态)
- [快速冒烟测试](#快速冒烟测试)
- [API 参考](#api-参考)
- [常见问题](#常见问题)

---

## 环境要求

| 依赖 | 最低版本 | 安装方式 |
|------|---------|---------|
| JDK | 21 | `brew install openjdk@21` 或 [Adoptium](https://adoptium.net/) |
| Maven | 3.6 | 项目自带 `mvnw` wrapper，无需全局安装 |
| Node.js | 20 | `brew install node` |
| MySQL | 8.0 | `brew install mysql` |
| Redis | 6.0 | `brew install redis && brew services start redis` |
| Docker | 20.10 | [Docker Desktop](https://www.docker.com/products/docker-desktop/) 或 colima |

> **macOS Homebrew Java 路径问题**：Homebrew 安装的 JDK 不会自动加入 PATH。启动 Spring Boot 前需要：
> ```bash
> export JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home
> export PATH=$JAVA_HOME/bin:$PATH
> ```

---

## 初始化步骤

### 1. 克隆项目

```bash
git clone https://github.com/haotangyuan/KnowNote.git
cd KnowNote
```

### 2. 启动 Docker 中间件

项目通过 `docker-compose.yml` 管理 RocketMQ 和 MinIO：

```bash
docker-compose up -d
```

| 容器 | 端口 | 用途 |
|------|------|------|
| `rmq-namesrv` | 9876 | RocketMQ 注册中心 |
| `rmq-broker` | 10911 / 10909 | RocketMQ 消息代理 |
| `minio` | 9000 / 9001 | MinIO 对象存储（9000=API，9001=控制台） |

**首次启动后创建 MinIO 存储桶（执行一次）：**

```bash
docker exec minio mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec minio mc mb local/knownote-public local/knownote-private
```

MinIO 控制台：http://localhost:9001，账号/密码均为 `minioadmin`。

### 3. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS db_knownote
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

应用首次启动时会通过 `src/main/resources/schema.sql` 自动建表，无需手动执行 DDL。

### 4. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，按下表填写所需变量：

#### Spring Boot 后端变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_USERNAME` | — | **必填**，MySQL 用户名 |
| `DB_PASSWORD` | — | **必填**，MySQL 密码 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码（无认证时留空） |
| `JWT_SECRET` | `knownote-dev-secret-key-...` | JWT 签名密钥（生产环境必须更换） |
| `ROCKETMQ_NAME_SERVER` | `localhost:9876` | RocketMQ 注册中心地址 |
| `OSS_ENDPOINT` | `http://localhost:9000` | MinIO API 地址 |
| `OSS_PUBLIC_URL` | `http://localhost:9000` | MinIO 对外访问地址（CDN 时不同） |
| `OSS_PUBLIC_BUCKET` | `knownote-public` | 公开 Bucket 名 |
| `OSS_PRIVATE_BUCKET` | `knownote-private` | 私有 Bucket 名 |
| `OSS_ACCESS_KEY_ID` | `minioadmin` | MinIO Access Key |
| `OSS_SECRET_ACCESS_KEY` | `minioadmin` | MinIO Secret Key |
| `RESEARCH_MODEL` | — | 深度研究用 LLM 模型名，如 `deepseek-chat` |
| `RESEARCH_MODEL_BASE_URL` | — | LLM API 地址，如 `https://api.deepseek.com` |
| `RESEARCH_MODEL_API_KEY` | — | LLM API Key（不配则 AI 功能调用时报错） |
| `TAVILY_API_KEY` | — | Tavily 搜索 API Key |
| `REVIEW_AI_ENABLED` | `false` | 是否启用 LLM 内容审核 |
| `REVIEW_AI_BASE_URL` | — | 审核 LLM API 地址 |
| `REVIEW_AI_API_KEY` | — | 审核 LLM API Key |
| `REVIEW_AI_MODEL` | — | 审核 LLM 模型名 |
| `RESEND_API_KEY` | 空 | Resend 邮件 API Key（不配则验证码打印到日志） |
| `RESEND_FROM` | 空 | 发件邮箱地址 |
| `GOOGLE_CLIENT_ID` | 空 | Google OAuth Client ID（不配则 Google 登录不可用） |
| `GOOGLE_CLIENT_SECRET` | 空 | Google OAuth Secret |
| `GOOGLE_REDIRECT_URI` | 空 | Google OAuth 回调地址 |
| `STUDIO_SERVICE_URL` | `http://localhost:3001` | Studio Service 地址 |
| `TIME_TIMEZONE` | `Asia/Shanghai` | 应用时区 |

> **最小启动配置**：只需填写 `DB_USERNAME`、`DB_PASSWORD`，其余使用默认值即可启动（AI 功能调用时会报错，基础功能正常）。

#### Studio Service 变量（`studio-service/.env`）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PORT` | `3001` | 服务端口 |
| `DOCKER_SOCKET` | `/var/run/docker.sock` | Docker Unix Socket 路径 |
| `WORKSPACE_BASE` | `/tmp/knownote-studio` | 沙盒工作区根目录（宿主机） |
| `SANDBOX_IMAGE` | `knownote-studio-sandbox:latest` | 沙盒镜像名 |
| `HOST_NAME` | `localhost` | 宿主机名称 |

> **macOS + colima**：`DOCKER_SOCKET` 设为 `~/.colima/default/docker.sock`；`WORKSPACE_BASE` 必须在 `$HOME` 下（colima 默认只挂载用户目录到虚拟机）。

---

## 启动服务

### Spring Boot 后端

```bash
# 加载 .env 并启动（项目根目录）
set -a && source .env && set +a && ./mvnw spring-boot:run
```

启动后：

| 地址 | 说明 |
|------|------|
| `http://localhost:8080` | REST API |
| `http://localhost:8080/docs` | Scalar API 文档（无需登录） |
| `http://localhost:8080/actuator/health` | 健康检查，返回 `{"status":"UP"}` |

### Studio Service（Node.js）

Studio 功能依赖 studio-service 和沙盒 Docker 镜像。

**第一次使用：构建沙盒镜像**

```bash
cd sandbox-image
docker build -t knownote-studio-sandbox:latest .
cd ..
```

**启动 studio-service**

```bash
cd studio-service
npm install

# 创建 .env（根据实际环境修改）
cat > .env << 'EOF'
PORT=3001
DOCKER_SOCKET=/var/run/docker.sock
WORKSPACE_BASE=/tmp/knownote-studio
SANDBOX_IMAGE=knownote-studio-sandbox:latest
HOST_NAME=localhost
EOF

npm run dev    # 开发模式（tsx watch 热重载）
```

健康检查：`curl http://localhost:3001/health` → `{"ok":true}`

### Studio 前端（React）

```bash
cd studio-frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。

> 前端通过 Vite proxy 将 `/api` 转发到 `:8080`，将 `/studio-ws` 转发到 `:3001`，开发时无需处理跨域。

---

## 功能启用状态

| 功能 | 依赖 | 最小启动可用？ |
|------|------|--------------|
| 密码注册 / 登录 | MySQL + Redis | ✅ |
| 验证码注册 / 登录 | + Resend | ⚠️ 未配 Resend 时验证码打印到日志 |
| Google OAuth 登录 | Google Client ID/Secret | ❌ 需自行配置 |
| 文章 CRUD | MySQL + MinIO | ✅ |
| 文章发布 / 内容审核 | + RocketMQ + (REVIEW_AI) | ✅ 审核结果取决于 `REVIEW_AI_ENABLED` |
| 点赞 | RocketMQ + Redis | ✅ |
| 深度研究 | LLM API + Tavily | ❌ 需配置 API Key |
| Studio 项目管理 | MySQL + studio-service | ✅ |
| Studio AI 代码生成 | + LLM API | ❌ 需配置 API Key |
| Studio 沙盒预览 | Docker + studio-service | ✅ |

---

## 快速冒烟测试

```bash
# 1. 健康检查
curl http://localhost:8080/actuator/health

# 2. 发送验证码（未配 Resend 时验证码打印在日志）
curl -X POST http://localhost:8080/api/v1/auth/code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'

# 3. 注册
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","username":"testuser","nickname":"测试用户","password":"123456","code":"<日志中的验证码>"}'

# 4. 密码登录，获取 Token
curl -X POST http://localhost:8080/api/v1/auth/login/password \
  -H "Content-Type: application/json" \
  -d '{"account":"test@example.com","password":"123456"}'
# 响应中取 accessToken

# 5. 获取当前用户信息
curl http://localhost:8080/api/v1/user/me \
  -H "Authorization: Bearer <accessToken>"

# 6. Studio 健康检查
curl http://localhost:8080/api/v1/studio/health
```

---

## API 参考

完整交互式文档见 `http://localhost:8080/docs`（Scalar UI），支持在线调试和 Bearer Token 认证。

### 认证 `/api/v1/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/code` | 发送邮箱验证码 | 无 |
| POST | `/register` | 邮箱验证码注册 | 无 |
| POST | `/login/password` | 密码登录 | 无 |
| POST | `/login/code` | 验证码登录 | 无 |
| POST | `/login/google` | Google One Tap 登录 | 无 |
| POST | `/google/callback` | Google OAuth 回调 | 无 |
| POST | `/refresh` | 刷新 Access Token | Refresh Token |
| POST | `/logout` | 登出（使 Refresh Token 失效） | Bearer |

### 用户 `/api/v1/user`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/me` | 获取当前用户信息 | Bearer |
| PUT | `/profile` | 更新昵称 / 头像 / 简介 | Bearer |
| PUT | `/password` | 修改密码 | Bearer |

### 深度研究 `/api/v1/research`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/create` | 创建研究会话 | Bearer |
| POST | `/{id}/messages` | 发送消息（SSE 流式进度） | Bearer |
| GET | `/{id}/events` | SSE 事件流（断线重连） | Bearer |
| GET | `/list` | 获取研究会话列表 | Bearer |

### 文章 `/api/v1/post`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/create` | 创建文章 | Bearer |
| POST | `/{id}/content` | 保存正文（MinIO 路径） | Bearer |
| POST | `/{id}/metadata` | 保存标题 / 标签 / 封面等元数据 | Bearer |
| POST | `/{id}/publish` | 提交发布（触发异步审核） | Bearer |
| POST | `/{id}/unpublish` | 下架 | Bearer |
| POST | `/{id}/delete` | 删除 | Bearer |
| POST | `/{id}/rollback` | 回滚至指定历史版本 | Bearer |
| GET | `/{id}` | 获取文章详情 | Bearer |
| GET | `/{id}/versions` | 获取版本历史列表 | Bearer |

### 点赞 `/api/v1/like`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/post` | 点赞 / 取消点赞（幂等） | Bearer |
| GET | `/status` | 批量查询点赞状态 | Bearer |

### 存储 `/api/v1/oss`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/url` | 获取 MinIO 预签名上传 URL | Bearer |

### Studio `/api/v1/studio`

> Studio 接口当前已加入 JWT 白名单，开发阶段无需 Token；生产部署时应移除白名单。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/health` | Studio 联通性健康检查 | 无 |
| POST | `/projects` | 创建 Studio 项目 | 无（开发） |
| GET | `/projects` | 列出所有项目 | 无（开发） |
| GET | `/projects/{projectId}` | 获取项目详情 | 无（开发） |
| POST | `/projects/{projectId}/generate` | AI 代码生成（SSE 流式） | 无（开发） |
| GET | `/projects/{projectId}/events` | SSE 重连（Last-Event-ID） | 无（开发） |
| GET | `/projects/{projectId}/container/status` | 获取沙盒容器状态 | 无（开发） |

Studio Service 内部 API 详见 [studio-service/README.md](./studio-service/README.md)。

---

## 常见问题

### Spring Boot 启动报 "Unable to locate a Java Runtime"

macOS Homebrew 安装的 JDK 未自动加入 `PATH`，在启动命令前执行：

```bash
export JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### studio-service 报 "Cannot connect to Docker socket"

- **Docker Desktop**：确认 Docker 已启动，默认 socket 路径 `/var/run/docker.sock` 正确。
- **colima**：将 `DOCKER_SOCKET` 设为 `~/.colima/default/docker.sock`，并确认 colima 正在运行（`colima status`）。

### Studio 沙盒容器启动失败 / 文件挂载失败（colima）

colima 默认只把 `$HOME` 挂载进虚拟机。将 `WORKSPACE_BASE` 设置为 `$HOME` 下的目录，例如：

```bash
WORKSPACE_BASE=/Users/yourname/knownote-studio
```

### 验证码收不到邮件

未配置 `RESEND_API_KEY` 时，验证码会打印在 Spring Boot 应用日志中，搜索关键字 `verification code` 即可找到。

### RocketMQ 消费者连接超时

确认 `docker-compose up -d` 已正常启动 `rmq-namesrv` 和 `rmq-broker` 两个容器：

```bash
docker ps | grep rmq
```

若 broker 未就绪，等待约 30 秒后重启 Spring Boot 应用。

### MinIO 上传返回 403

确认已创建两个存储桶：

```bash
docker exec minio mc ls local/
```

若不存在，重新执行：

```bash
docker exec minio mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec minio mc mb local/knownote-public local/knownote-private
```
