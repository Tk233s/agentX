/*
 API Key 管理 - 建表脚本
 一个用户可以配置任意多个具名 API Key。
 provider 表示接口协议（openai/anthropic），不是供应商名称；
 同一协议下可创建多个 Key，并通过 name 和 base_url 区分。
 Agent 通过 api_key_id 关联到具体 Key。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

use `agentx`;

-- ----------------------------
-- Table structure for api_key
-- ----------------------------
DROP TABLE IF EXISTS `api_key`;
CREATE TABLE `api_key` (
  `id`           varchar(64)  NOT NULL COMMENT '主键ID',
  `user_id`      varchar(64)  NOT NULL COMMENT '所属用户ID',
  `name`         varchar(128) NOT NULL COMMENT '密钥名称',
  `provider`     varchar(64)  NOT NULL COMMENT '接口协议 openai/anthropic',
  `api_key`      varchar(512) NOT NULL COMMENT 'API密钥',
  `base_url`     varchar(512) DEFAULT NULL COMMENT '自定义API地址（代理/中转）',
  `enabled`      tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
  `created_at`   datetime     NOT NULL COMMENT '创建时间',
  `updated_at`   datetime     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API密钥表';
