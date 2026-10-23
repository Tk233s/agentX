# agentX

一个从零实现的 Agent 对话平台。用 DDD 六边形架构分层，把 Agent 的几个核心机制自己实现了一遍：
多轮对话与上下文压缩、工具调用运行时、会话级权限控制、RAG 知识库检索。

## 技术栈

| 用途 | 选型 |
| --- | --- |
| 语言 / 框架 | Java 17、Spring Boot 3 |
| 大模型接入 | Spring AI + OpenAI 兼容接口（DeepSeek 负责对话，bge-m3 负责向量化） |
| 业务数据 | MySQL 8 + MyBatis |
| 向量检索 | PostgreSQL + pgvector 0.5.1（HNSW 索引） |
| 关键词检索 | PostgreSQL pg_trgm（`word_similarity`） |
| 前端 | 原生 JS + Node 内置 `http`（零依赖，只做静态托管与 `/api` 反向代理） |

## 已实现的功能

| 能力 | 说明 |
| --- | --- |
| 账号与鉴权 | 用户名口令登录，签发 JWT，接口统一走拦截器校验 |
| Agent 管理 | 创建 / 编辑 / 删除 / 查询 Agent，配置提示词、模型与能力模式 |
| 会话管理 | 多会话、自动标题、重命名、会话级权限档、删除 |
| 流式对话 | SSE 逐字输出，支持中断，消息持久化并携带引用来源 |
| 上下文压缩 | 滑动窗口 + 摘要压缩，会话级累计 token 上限软限制 |
| 工具调用 | 注解注册工具，模型自主调用，调用记录全量审计 |
| 会话权限 | 能力模式 × 会话权限档两维收缩，写操作在执行点二次拦截 |
| 知识库 | 多知识库、文档上传与异步入库、检索阈值与 TopK 可配、检索调试 |
| 检索质量评测 | 离线标注集 + 评测运行器，可对多组检索配置批量打分 |

## 架构

六层单向依赖，外层依赖内层，领域层不认识任何框架：

```
agentX-trigger        HTTP 入口，只做鉴权、参数校验与响应装配
agentX-app            启动装配、配置、全局过滤器、启动任务
agentX-api            对外 DTO 与 Assembler
agentX-domain         领域实体、领域服务、端口接口（纯业务，无框架实现）
agentX-infrastructure 端口实现：MySQL / PostgreSQL 仓储、Spring AI、工具运行时、文件存储
agentX-types          统一返回结构、错误码、异常
agentX-web            前端
```

领域层通过端口接口（`EmbeddingPort`、`VectorStorePort`、`DocumentParserPort`、`DocumentTaskPort` 等）
声明需要的外部能力，基础设施层提供实现。换向量库、换模型、换文件存储，改动都落在 `agentX-infrastructure`，
领域逻辑一行不动。

## 使用流程

### 1. 登录

账号由建表脚本初始化（本机初始数据为 `admin / AgentX@123456`），登录接口签发 JWT，
前端把 token 存在本地，后续请求自动附带；后端由拦截器统一校验，失效则要求重新登录。

### 2. 建 Agent

在侧边栏创建 Agent，填名称、系统提示词，并选择一个**能力模式**：

| 模式 | 挂载的工具 |
| --- | --- |
| `CHAT` | 不挂载文件类工具，只能对话与检索知识库 |
| `CODE_READONLY` | 挂 `file-read`、`weather`，可读不可写 |
| `CODE` | `file-read` + `file-write` 全挂 |
| `CUSTOM` | 由用户逐个勾选 |

### 3. 建会话并对话

会话上可以设置**权限档**（`READ_ONLY` / `FULL`）和绑定知识库。对话走 SSE 流式接口，
回答里的引用来源会随消息一起落库，前端展示引用角标。

### 4. 用知识库

1. 新建知识库，设置检索阈值与 TopK（默认 `minScore=0.55`、`topK=3`，只影响后续对话，不需要重新向量化）
2. 上传文档（支持 `.txt` / `.md` / `.markdown`），接口立即返回 `PENDING` 记录
3. 文档在后台完成解析 → 分片 → 向量化，状态流转 `PENDING → PROCESSING → READY / FAILED`
4. 列表里可以看到每个文档的分片数与状态，失败的可以点重试
5. 「检索调试」可以直接输入问题看召回结果与分数，不用先发起对话

### 5. 看工具审计

客户端可以列出已注册工具、单测某个工具、按会话查调用记录（参数、返回结果、耗时、成功与否）。

### 接口一览

| 分组 | 路径 |
| --- | --- |
| 鉴权 | `POST /auth/login` |
| Agent | `POST /agent/{create,update,delete}`、`GET /agent/{get,list}` |
| 会话 | `POST /session/{create,rename,permission,delete}`、`GET /session/{get,list}` |
| 对话 | `POST /conversation/chat`、`POST /conversation/stream`、`GET /message/list` |
| 知识库 | `POST /rag/knowledge-base/{create,retrieval-config,delete}`、`GET /rag/knowledge-base/{list,get}` |
| 文档 | `POST /rag/knowledge-base/document/{upload,retry,delete}`、`GET /rag/knowledge-base/document/{get,list}` |
| 检索 | `POST /rag/knowledge-base/search` |
| 工具 | `GET /tool/{list,calls}`、`POST /tool/test` |
| 密钥 | `POST /apikey/{create,update,delete}`、`GET /apikey/{get,list}` |

## 核心机制

### 1. 上下文：滑动窗口 + 摘要压缩

长对话必然撑爆上下文窗口。做法是分级处理：先用滑窗装下尽可能多的历史；超出阈值时，
把较早的对话交给模型压缩成摘要，只把摘要加最近几组原文发给模型。

- 摘要与消息用 `summary_through_message_id` 水位对齐，重算时不会重复压缩
- 水位推进加乐观锁，冲突时重读一次再试
- 摘要本身消耗的 token 计入会话额度，额度耗尽后降级回纯滑动窗口

会话级还有累计 token 上限（默认无限），是软限制：跨线后在下一次请求返回 429，不打断正在生成的回答。

### 2. 工具运行时

工具用 `@AgentTool` 注解声明（`value` 是工具 ID，另有名称、描述、是否产生副作用），
启动时扫描出元数据和执行回调，注册进 `ToolRegistry`。模型按需自主调用，
调用路径上统一包一层审计：成功与失败各落一条 `tool_call_record`，记录参数、返回结果与耗时。

工具循环本身交给 Spring AI 托管，项目不自己实现 ReAct 循环——这一层的复杂度不值得重复造。

已内置三个示例工具：`file-read`（受限目录内列目录 / 读文本）、`file-write`（受限目录内写文本）、
`weather`（演示 Function Calling 的模拟数据源）。

### 3. 会话级权限：只能做减法

权限拆成两个维度：

- **Agent 能力模式**：`CHAT` / `CODE_READONLY` / `CODE` / `CUSTOM`，决定这个 Agent 默认挂哪些工具
- **会话权限档**：`READ_ONLY` / `FULL`，决定这次对话能用到什么程度

会话权限只能在 Agent 给定范围内**收缩**，无法突破 Agent 自身的能力边界。
文件工具拆成 `file-read` 与 `file-write` 两个 ID，只读会话根本不挂写工具；
即使绕过了挂载，`mutating` 标记的工具在执行点还有二次拦截并落审计。

### 4. RAG 检索

```
query ──┬─→ 向量路：pgvector 余弦相似度 + HNSW ──┐
        └─→ 关键词路：pg_trgm word_similarity ──┴─→ RRF 融合 ──→ 可信度判定 ──→ topK
```

- **两路并行召回**：向量路负责语义相近，关键词路负责字面精确（编号、专有名词、错误码）
- **RRF 融合**：用 `1/(k+rank)` 累加两路排名，两路权重可配，只依赖名次、不依赖分值量纲
- **可信度判定**：融合之后再看各路**原始分数**。向量分是余弦相似度可以直接采信；
  关键词分只是字面重合度，需要独立的高阈值（`MIN_KEYWORD_CONFIDENT_SCORE=0.60`）才算证据。
  两路都没给出强证据时返回空结果，按「查不到」处理，不让模型拿着不相关分片编答案

异步入库：上传接口只落一条 `PENDING` 记录即刻返回，解析与向量化交给后台线程池。
应用启动时会扫一遍静默超过 10 分钟的 `PROCESSING` 文档并改判为 `FAILED`，
保证进程重启后不会留下永远停在「处理中」的记录。

## 检索质量评测

仓库里带一套离线评测设施（`RetrievalEvalRunner`）：8 份语料共 46 个分片，
30 条人工标注题覆盖精确匹配 / 同义改述 / 跨分片 / 无答案四类，锚定到原文片段。
运行器可以一次扫描多组检索配置，输出 Hit@3、Recall@3、MRR、拒答正确率与平均延迟，
报告落在 `docs/eval/`。改检索逻辑后跑一遍就能对比回归。

当前记录的三组结果：

| 配置 | Hit@3 | Recall@3 | MRR | 拒答正确率 | 平均延迟 |
| --- | --- | --- | --- | --- | --- |
| 纯向量（v1.1） | 0.870 | 0.870 | 0.645 | 0.429 | 165 ms |
| 混合检索 1:1（融合口令，拒答判定调整前） | 0.826 | 0.826 | 0.630 | 0.143 | 159 ms |
| 混合检索 1:1（当前默认） | 0.826 | 0.826 | 0.630 | 0.429 | 159 ms |

前两行的差别只在拒答判定：早期用「融合结果为空」当拒答条件，而 RRF 分值只含排名信息
（值域约 0.016~0.033），中文 trigram 分数又偏松，无答案题照样能召回候选，
于是「空结果」几乎永不成立。改成按各路原始分数判定后，拒答正确率回到基线水平，
Hit@3 与 MRR 完全没动——这也是为什么评测要同时看召回、排序和拒答三类指标：
单看 Hit@3，拒答这条链路的偏差一分都不扣。

指标含义：

- **Hit@3**：正确分片有没有进前 3 条，衡量召回
- **Recall@3**：应找回的分片找回了多少，衡量覆盖
- **MRR**：命中位置的平均倒数排名（rank1=1.00、rank2=0.50…），衡量排序质量
- **拒答正确率**：库里确实没有答案的题，是否返回了空结果

运行方式（需要真实 PostgreSQL 与 embedding key）：

```bash
mvn -o dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=agentX-app/target/cp-full.txt
java @agentX-app/target/run-cp.args -Drag.eval=true org.example.rag.RetrievalEvalRunner
```

首次运行会把自己的语料灌进一个专用知识库，之后复用已有分片。

## 快速开始

**环境要求**：JDK 17、Maven、MySQL 8、PostgreSQL（装 pgvector 扩展）、Node 18+

1. 建库：MySQL 建 `agentx`，PostgreSQL 建 `agentx_rag`

2. 执行建表脚本：MySQL 先按顺序执行 `db/mysql/base/`，再按序号执行 `db/mysql/migration/`
   （`archive/` 是已并入 base 的历史迁移，跳过）；PostgreSQL 先建库与 `vector` 扩展，
   再执行 `db/postgresql/012_rag_pgvector.sql`。完整顺序见 `db/README.md`

3. 配置环境变量（程序从环境读取，不落配置文件）：

```bash
JWT_SECRET=至少32位的随机字符串
DS_API_KEY=大模型服务商的 key
RAG_DB_PASSWORD=PostgreSQL 口令
```

4. 启动后端（默认 8091）：

```bash
mvn -DskipTests install
java -jar agentX-app/target/agentX-app-1.0-SNAPSHOT.jar
```

5. 启动前端（默认 5173，`/api` 代理到 8091）：

```bash
cd agentX-web && npm run dev
```

前端细节见 `agentX-web/README.md`。

## 测试

```bash
mvn test -Dskip.unit.tests=false
```

默认 `mvn install` 会跳过测试，这样构建不依赖数据库与外部 key；需要跑用例时用上面的命令打开。
当前 83 个用例，其中 1 个上下文装配冒烟用例（`ApiTest`）标注 `@Ignore`，
因为它需要完整的数据库与密钥环境，适合本地手动启用。
