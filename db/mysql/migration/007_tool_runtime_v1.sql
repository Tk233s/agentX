use `agentx`;

CREATE TABLE IF NOT EXISTS tool_call_record (
    id VARCHAR(64) NOT NULL COMMENT '调用记录ID',
    tool_id VARCHAR(64) NOT NULL COMMENT '工具ID',
    method_name VARCHAR(128) NOT NULL COMMENT '工具方法名',
    user_id VARCHAR(64) NULL COMMENT '调用用户ID',
    session_id VARCHAR(64) NULL COMMENT '所属会话ID',
    arguments_json TEXT NULL COMMENT '调用参数JSON',
    result_text MEDIUMTEXT NULL COMMENT '调用结果文本',
    success TINYINT NOT NULL COMMENT '是否成功：1成功，0失败',
    error_message VARCHAR(1000) NULL COMMENT '失败原因',
    latency_ms BIGINT NOT NULL COMMENT '执行耗时（毫秒）',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_session_user_created (session_id, user_id, created_at),
    INDEX idx_tool_created (tool_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具调用审计记录';
