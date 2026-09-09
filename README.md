# 智答ai

智答ai是由 `chisa` 独立设计与实现的 Spring AI 全栈应用。项目包含本地 Ollama 对话、SSE 流式输出、Prompt/Advisor 相关接口、历史对话、Markdown 知识库客服、联网搜索和大文件分片上传。

## 目录

- `src/main/java/chisa/zhida`：Spring Boot 后端，IDEA 可直接以 Maven 项目打开。
- `frontend`：Vue 3 + Vite + Tailwind CSS 4 + Ant Design Vue + Pinia 前端。
- `docker-compose.yml`：PostgreSQL/PGVector、Redis Stack、RabbitMQ、SearXNG 开发服务。
- `docker`：本地依赖服务的持久化目录和 SearXNG 配置。

## 运行

1. 确认 `java -version` 为 21；本机 Ollama 安装在 `D:\ollama` 时，启动 Ollama 并确认 `qwen3:1.7b`、`nomic-embed-text` 已存在。模型文件使用 `D:\ollama\.ollama\models`，不会写入 C 盘。
2. 打开 Docker Desktop，然后执行 `docker compose up -d`，启动 PostgreSQL/PGVector、Redis Stack、RabbitMQ、Cassandra 和 SearXNG。
3. 在 IDEA 打开根目录，运行 `chisa.zhida.ZhidaAiApplication`，或执行 `mvn spring-boot:run`。默认 profile 是 `ollama`，模型请求会进入本机 Ollama。D 盘存在 `D:\Environment\zhida\application-local.yml` 时，应用会自动读取其中的私有数据库、阿里云、智谱和高德 MCP 配置；该文件不会进入 Git。没有该文件时，外部云模型和 MCP 保持关闭。切换到阿里百炼配置可使用 `SPRING_PROFILES_ACTIVE=dev`，并提供 `OPENAI_API_KEY`。
4. 在 `frontend` 执行 `npm install`、`npm run dev`，浏览器访问 `http://localhost:5173`。

PostgreSQL 使用 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量配置连接，密码不会写入仓库。最终工程不使用 H2 或手写 HTTP 调用替代 Spring AI。

## Ollama / 外部模型

默认配置：`OLLAMA_BASE_URL=http://127.0.0.1:11434`、`OLLAMA_MODEL=qwen3:1.7b`、`OLLAMA_EMBEDDING_MODEL=nomic-embed-text`。聊天模型负责问答，embedding 模型负责 PGVector 检索；也可以通过环境变量切换模型。DeepSeek、OpenAI、百炼兼容接口的地址和 Key 分别由 `OPENAI_BASE_URL`、`OPENAI_API_KEY`、`OPENAI_MODEL` 管理，不把密钥提交到仓库。

依赖下载命令（使用你指定的 Maven 仓库）：

```cmd
cd /d F:\Project\zhida
D:\Environment\apache-maven-3.9.16\bin\mvn.cmd -s D:\Environment\apache-maven-3.9.16\conf\settings.xml -Dmaven.repo.local=D:\Environment\apache-maven-3.9.16\repo dependency:go-offline
cd frontend
npm install --cache D:\Environment
pm-cache
```

也可以直接在项目根目录执行 `download-dependencies.cmd`，它会依次完成以上两步。

Ollama 程序位于 `D:\ollama`，当前模型缓存为 `D:\ollama\.ollama\models`。启动 Ollama 后执行 `ollama pull qwen3:1.7b` 和 `ollama pull nomic-embed-text`；下载前请确认 Ollama 的 `OLLAMA_MODELS` 仍指向该目录。针对 3050 Ti 4GB 显存 + 16GB 内存，默认使用 `qwen3:1.7b`。如果 Ollama 自动使用 GPU 后出现 `cudaMalloc failed: out of memory`，需要在启动 Ollama 服务的同一个终端设置 `OLLAMA_LLM_LIBRARY=cpu`，或先释放显存和系统提交内存；仅在请求中设置 `num_gpu=0` 不保证桌面服务切换到 CPU。

MCP Client 使用 D 盘私有配置 `D:\Environment\zhida\mcp-servers-config.json` 和 `application-local.yml`，高德 Key 不应写入仓库。私有配置存在时会自动启用高德 MCP；也可以用 `ZHIDA_MCP_CLIENT_ENABLED=false` 临时关闭。启动后访问 `/api/mcp/status`，应看到高德工具已注册；本地自定义 MCP Server 仍默认注册 QQ 工具。

## 主要接口

- `GET /api/health`
- `GET /api/ai/generate`、`GET /api/v3/ai/generateStream`
- `POST /api/chat/new`、`POST /api/chat/completion`（SSE）
- `GET /api/chat/history`、`GET /api/chat/{chatId}/messages`
- `POST /api/customer-service/chat/completion`（SSE）
- `GET /api/v2/ai/generate`、`GET /api/v2/ai/generateStream`（DeepSeek profile）
- `GET /api/v5/ai/generate`、`GET /api/v5/ai/generateStream`（OpenAI profile）
- `GET /api/v6/ai/generate`、`GET /api/v6/ai/generateStream`（百炼 OpenAI-compatible profile）
- `GET /api/tools/generateStream`、`GET /api/lab/chat-memory/generateStream`
- `GET /api/lab/chat-client/generate`、`GET /api/lab/chat-client/generateStream`
- `GET /api/lab/prompt/template`、`GET /api/lab/prompt/role`
- `GET /api/lab/structured/actor-films`、`/language-info`、`/city-list`
- `GET /api/mcp/status`、`GET /api/mcp/generateStream`
- `GET /v10/ai/text2img`、`GET /v11/ai/text2audio`、`GET /v12/ai/text2video`
- `GET /v10/ai/media/{fileName}`（访问 D 盘保存的媒体文件）
- `GET /api/agent/support-agent/run?strategy=harness`
- `GET /api/read?path=...`、`GET /api/network/test?message=...`
- `POST /api/customer-service/md/upload`、`GET /api/customer-service/md/list`、`DELETE /api/customer-service/md/{id}`
- `POST /api/file/check`、`POST /api/file/chunk`、`POST /api/file/merge`

项目使用的 Redis/MQ 需要 Docker Desktop 运行；如果 Docker CLI 报无法连接 daemon，请打开 Docker Desktop 后再执行 `docker compose up -d`。
