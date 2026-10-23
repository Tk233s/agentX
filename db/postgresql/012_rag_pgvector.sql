-- RAG 知识库 v2：知识库三张表整体迁到 PostgreSQL，向量用 pgvector 原生类型。
-- 前置：库已建好并执行过 CREATE EXTENSION IF NOT EXISTS vector;
-- 向量维度与 bge-m3 对齐（1024）；换向量化模型要重建 embedding 列并重新入库。

CREATE TABLE IF NOT EXISTS knowledge_base (
    id              VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    api_key_id      VARCHAR(64)  NOT NULL,
    embedding_model VARCHAR(128) NOT NULL,
    dimension       INT          NOT NULL DEFAULT 1024,
    chunk_size      INT          NOT NULL DEFAULT 500,
    chunk_overlap   INT          NOT NULL DEFAULT 50,
    created_at      TIMESTAMP(6) NOT NULL,
    updated_at      TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_kb_user ON knowledge_base (user_id);

CREATE TABLE IF NOT EXISTS knowledge_document (
    id                VARCHAR(64)  NOT NULL,
    knowledge_base_id VARCHAR(64)  NOT NULL,
    user_id           VARCHAR(64)  NOT NULL,
    file_name         VARCHAR(255) NOT NULL,
    file_path         VARCHAR(512) NOT NULL,
    file_size         BIGINT       NOT NULL DEFAULT 0,
    file_type         VARCHAR(32),
    char_count        INT          NOT NULL DEFAULT 0,
    chunk_count       INT          NOT NULL DEFAULT 0,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    error_message     VARCHAR(512),
    created_at        TIMESTAMP(6) NOT NULL,
    updated_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_doc_kb ON knowledge_document (knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_doc_user ON knowledge_document (user_id);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id                VARCHAR(64)  NOT NULL,
    knowledge_base_id VARCHAR(64)  NOT NULL,
    document_id       VARCHAR(64)  NOT NULL,
    user_id           VARCHAR(64)  NOT NULL,
    chunk_index       INT          NOT NULL,
    content           TEXT         NOT NULL,
    embedding         vector(1024) NOT NULL,
    char_count        INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_chunk_doc ON knowledge_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_kb ON knowledge_chunk (knowledge_base_id);

-- HNSW 需要 pgvector >= 0.5；分片量小的时候全表扫描也够用，可以先不建。
CREATE INDEX IF NOT EXISTS idx_chunk_vec ON knowledge_chunk USING hnsw (embedding vector_cosine_ops);
