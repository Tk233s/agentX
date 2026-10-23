ALTER TABLE session
    ADD COLUMN token_limit BIGINT NULL COMMENT '会话累计Token上限，NULL表示不限制' AFTER user_id,
    ADD COLUMN used_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '会话累计已使用Token' AFTER token_limit;

ALTER TABLE agent
    DROP COLUMN max_tokens;
