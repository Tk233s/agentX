/*
 用户表 - 建表脚本
 只有登录接口使用这张表，注册入口未开放，账号通过 SQL 初始化。
 口令存储格式：pbkdf2-sha256:迭代次数:base64(salt):base64(hash)
*/

SET NAMES utf8mb4;

use `agentx`;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`            varchar(64)  NOT NULL COMMENT '用户ID',
  `username`      varchar(64)  NOT NULL COMMENT '用户名',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希 pbkdf2-sha256:iterations:salt:hash',
  `created_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ----------------------------
-- 初始账号 admin / AgentX@123456
-- 哈希由 PasswordHasher.hash("AgentX@123456") 生成（PBKDF2-HMAC-SHA256，120000 轮）
-- ----------------------------
INSERT IGNORE INTO `user` (`id`, `username`, `password_hash`)
VALUES ('0f2f1a26b89d4b63a6cbcc470a48f4a2', 'admin',
        'pbkdf2-sha256:120000:U6dbrp0s3wOWB8+oFf5CRQ==:sH7Xc0+CTK2f/6W1t9fSlFIIIiz4IC0EiXkGzFDT8V4=');
