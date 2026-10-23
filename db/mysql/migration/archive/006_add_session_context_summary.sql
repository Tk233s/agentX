use `agentx`;

ALTER TABLE session
    ADD COLUMN context_summary LONGTEXT NULL COMMENT '较早对话的压缩摘要' AFTER used_tokens,
    ADD COLUMN summary_through_message_id VARCHAR(64) NULL COMMENT '摘要已经覆盖到的最后一条消息ID' AFTER context_summary,
    ADD COLUMN summary_updated_at DATETIME(6) NULL COMMENT '摘要最后更新时间' AFTER summary_through_message_id;
