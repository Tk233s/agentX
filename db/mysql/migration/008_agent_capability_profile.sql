use `agentx`;

-- Agent 能力模式：工具由运行时按模式自动提供，tools_json 降级为高级覆盖项。
ALTER TABLE agent
    ADD COLUMN capability_profile VARCHAR(32) NOT NULL DEFAULT 'CHAT' COMMENT '能力模式：CHAT/CODE_READONLY/CODE/RESEARCH/CUSTOM'
        AFTER model_id;

-- 已有工具绑定的 Agent 按旧的显式绑定行为处理，避免升级后行为静默变化。
UPDATE agent
SET capability_profile = 'CUSTOM'
WHERE tools_json IS NOT NULL
  AND tools_json <> '[]';
