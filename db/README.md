# 数据库脚本

业务数据在 MySQL，向量数据在 PostgreSQL，两边分开维护。

```
db/
├── mysql/
│   ├── base/          建库建表：空库初始化时按顺序执行
│   ├── migration/     增量迁移：按文件名序号依次执行
│   └── migration/archive/  历史迁移，已并入 base，不再执行
└── postgresql/        RAG 知识库三张表
```

## MySQL（业务库 `agentx`）

### 全新初始化

空库按下面顺序执行，一步都不能跳过：

| 顺序 | 脚本 | 内容 |
| --- | --- | --- |
| 1 | `base/agent.sql` | 建库 `agentx` + `agent` 智能体表 |
| 2 | `base/apikey.sql` | `api_key` 模型密钥表 |
| 3 | `base/session.sql` | `session` 会话表 + `message` 消息表 |
| 4 | `base/user.sql` | `user` 用户表 + 初始账号 |
| 5 | `base/message_created_at_precision.sql` | 消息时间戳改 `datetime(6)`，保证同秒消息可稳定排序 |
| 6 | `migration/002_message_stream_metadata.sql` | 消息表补 prompt / completion token 等列 |
| 7 | `migration/007_tool_runtime_v1.sql` | `tool_call_record` 工具调用审计表 |
| 8 | `migration/008_agent_capability_profile.sql` | Agent 能力模式 |
| 9 | `migration/009_split_file_tool.sql` | 旧 `file` 工具展开为 `file-read` / `file-write` |
| 10 | `migration/010_session_permission_mode.sql` | 会话权限档 |
| 11 | `migration/011_rag_kb_binding_and_citations.sql` | Agent 绑定知识库、消息引用快照 |

执行方式（每个脚本自带 `use agentx;`）：

```bash
mysql -uroot -p < db/mysql/base/agent.sql
# ……按上表顺序逐个执行
```

> `base/` 里的建表脚本带 `DROP TABLE IF EXISTS`，只用于空库初始化；已有数据的库不要再跑。

### 增量迁移

`migration/` 下按序号往后加。序号不连续是有意的：`003`–`006` 的改动已经合并进了
`base/` 的建表语句，重复执行会报 `Duplicate column name`，因此移到 `archive/` 留作变更记录，
**新库和旧库都不需要再执行它们**。

`archive/` 里四条分别是：移除 Agent 采样参数、会话 token 上限、多 API Key 与绑定、上下文摘要水位。

### 结构校验

这套脚本在空库上从头执行过一遍，建出的 6 张表（`agent`、`api_key`、`session`、`message`、`user`、
`tool_call_record`）与开发库逐列比对（列名、类型、可空、默认值、注释）完全一致。

## PostgreSQL（向量库 `agentx_rag`）

```sql
CREATE DATABASE agentx_rag;
\c agentx_rag
CREATE EXTENSION IF NOT EXISTS vector;
```

再执行 `postgresql/012_rag_pgvector.sql`，建立 `knowledge_base`、`knowledge_document`、
`knowledge_chunk` 三张表：embedding 列 1024 维（与 bge-m3 对齐），走 HNSW 索引。

换向量化模型时要重建 embedding 列维度并重新入库，不能只改代码。

## 初始账号

`base/user.sql` 末尾写入一个演示账号：`admin / AgentX@123456`。

口令以 PBKDF2-HMAC-SHA256（120000 轮、16 字节随机 salt）存储，格式
`pbkdf2-sha256:轮数:base64(salt):base64(hash)`，与 `PasswordHasher` 的实现一致；
哈希值已用同一算法反向验算过可以登录。正式使用前请改掉这个口令。
