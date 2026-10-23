/*
 会话与消息 - 建表脚本
 Session 记录用户与Agent的一次会话，Message记录会话中的每一条消息。
 一个Session对应多条Message，删除Session时级联删除其下所有Message。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

use `agentx`;

-- ----------------------------
-- Table structure for session 会话表
-- ----------------------------
DROP TABLE IF EXISTS `session`;
CREATE TABLE `session` (
  `id`           varchar(64)  NOT NULL COMMENT '会话ID',
  `title`        varchar(128) DEFAULT NULL COMMENT '会话标题',
  `agent_id`     varchar(64)  NOT NULL COMMENT '关联的智能体ID',
  `user_id`      varchar(64)  NOT NULL COMMENT '所属用户ID',
  `token_limit`  bigint       DEFAULT NULL COMMENT '会话累计Token上限，NULL表示不限制',
  `used_tokens`  bigint       NOT NULL DEFAULT 0 COMMENT '会话累计已使用Token',
  `context_summary` longtext   DEFAULT NULL COMMENT '较早对话的压缩摘要',
  `summary_through_message_id` varchar(64) DEFAULT NULL COMMENT '摘要已经覆盖到的最后一条消息ID',
  `summary_updated_at` datetime(6) DEFAULT NULL COMMENT '摘要最后更新时间',
  `created_at`   datetime     NOT NULL COMMENT '创建时间',
  `updated_at`   datetime     NOT NULL COMMENT '更新时间（最后一条消息时间）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- ----------------------------
-- Table structure for message 消息表
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`           varchar(64)  NOT NULL COMMENT '消息ID',
  `session_id`   varchar(64)  NOT NULL COMMENT '所属会话ID',
  `role`         varchar(32)  NOT NULL COMMENT '消息角色 user/assistant',
  `content`      text         NOT NULL COMMENT '消息内容',
  `tokens`       int          DEFAULT 0 COMMENT '消息Token数',
  `created_at`   datetime(6)  NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
