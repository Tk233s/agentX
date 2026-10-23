use `agentx`;

-- 防止用户消息和助手消息落在同一秒时无法稳定排序
ALTER TABLE `message`
    MODIFY COLUMN `created_at` datetime(6) NOT NULL COMMENT '创建时间';
