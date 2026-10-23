-- API Key 改为可配置多条，并将 Agent 绑定到具体的 API Key。
-- 执行前请确认 api_key 表中每个 (user_id, provider) 组合只有一条旧数据。

ALTER TABLE api_key
    ADD COLUMN name varchar(128) NOT NULL DEFAULT '' COMMENT '密钥名称' AFTER user_id;

UPDATE api_key
SET name = CASE
    WHEN provider = 'openai' THEN 'OpenAI Key'
    WHEN provider = 'anthropic' THEN 'Anthropic Key'
    ELSE CONCAT(provider, ' Key')
END
WHERE name = '';

ALTER TABLE api_key
    MODIFY COLUMN name varchar(128) NOT NULL COMMENT '密钥名称',
    MODIFY COLUMN provider varchar(64) NOT NULL COMMENT '接口协议 openai/anthropic',
    DROP INDEX idx_user_provider,
    ADD INDEX idx_user_id (user_id);

ALTER TABLE agent
    ADD COLUMN api_key_id varchar(64) NULL COMMENT '绑定的API密钥ID' AFTER welcome_message;

UPDATE agent a
JOIN api_key k
  ON k.user_id = a.user_id
 AND k.provider = a.provider
SET a.api_key_id = k.id
WHERE a.api_key_id IS NULL;

ALTER TABLE agent
    MODIFY COLUMN api_key_id varchar(64) NOT NULL COMMENT '绑定的API密钥ID',
    DROP COLUMN provider,
    ADD INDEX idx_api_key_id (api_key_id);
