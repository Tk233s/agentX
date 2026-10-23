use `agentx`;

-- 会话权限档：能力模式决定"有哪些工具"，权限档决定"这次对话允许做到什么程度"。
-- 默认 FULL（放行），与升级前"会话能用到 Agent 的全部工具"行为保持一致，避免静默变化。
ALTER TABLE session
    ADD COLUMN permission_mode VARCHAR(32) NOT NULL DEFAULT 'FULL' COMMENT '会话权限档：READ_ONLY 只读、FULL 放行'
        AFTER used_tokens;
