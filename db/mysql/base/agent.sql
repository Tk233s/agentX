/*
 创建智能体业务 - 建库建表脚本
 注意：agentx 为占位库名，接入正式数据库后替换为真实库名（同步修改 application-dev.yml 中的 url）
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE database if NOT EXISTS `agentx` default character set utf8mb4 collate utf8mb4_0900_ai_ci;
use `agentx`;

-- ----------------------------
-- Table structure for agent 智能体表
-- ----------------------------
DROP TABLE IF EXISTS `agent`;
CREATE TABLE `agent` (
  `id`               varchar(64)  NOT NULL COMMENT '智能体ID',
  `name`             varchar(128) NOT NULL COMMENT '智能体名称',
  `avatar`           varchar(512) DEFAULT NULL COMMENT '智能体头像URL',
  `description`      varchar(512) DEFAULT NULL COMMENT '智能体描述',
  `system_prompt`    text         COMMENT '智能体系统提示词',
  `welcome_message`  text         COMMENT '欢迎消息',
  `api_key_id`       varchar(64)  NOT NULL COMMENT '绑定的API密钥ID',
  `model_id`         varchar(128) DEFAULT NULL COMMENT '模型ID',
  `tools_json`       text         COMMENT '工具名称列表 JSON 数组',
  `enabled`          tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
  `user_id`          varchar(64)  DEFAULT NULL COMMENT '创建者用户ID',
  `created_at`       datetime     NOT NULL COMMENT '创建时间',
  `updated_at`       datetime     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_api_key_id` (`api_key_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体表';
