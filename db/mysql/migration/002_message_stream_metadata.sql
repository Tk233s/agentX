use `agentx`;

-- 流式输出需要分别记录输入 / 输出 token，用于会话额度累计与展示。
ALTER TABLE message
    ADD COLUMN prompt_tokens INT NULL AFTER tokens,
    ADD COLUMN completion_tokens INT NULL AFTER prompt_tokens,
    ADD COLUMN total_tokens INT NULL AFTER completion_tokens,
    ADD COLUMN model VARCHAR(128) NULL AFTER total_tokens,
    ADD COLUMN provider VARCHAR(32) NULL AFTER model,
    ADD COLUMN finish_reason VARCHAR(32) NULL AFTER provider,
    ADD COLUMN latency_ms BIGINT NULL AFTER finish_reason,
    ADD COLUMN usage_source VARCHAR(16) NULL AFTER latency_ms;
