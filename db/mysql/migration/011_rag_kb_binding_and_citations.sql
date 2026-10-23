use `agentx`;

-- RAG v1：知识库与 Agent 绑定关系，以及消息上的引用快照。
-- 建表脚本（base/session.sql）里没有这两列，全新初始化时靠这条补上；
-- 已有库若之前手工加过，执行前先确认列是否已存在。

ALTER TABLE agent
    ADD COLUMN knowledge_base_ids text COMMENT '绑定的知识库 ID 列表' AFTER updated_at;

ALTER TABLE message
    ADD COLUMN citations_json longtext COMMENT '知识库检索引用 JSON' AFTER created_at;
