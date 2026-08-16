package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息持久化对象
 */
@Data
public class MessagePO {

    /** 消息ID */
    private String id;

    /** 所属会话ID */
    private String sessionId;

    /** 消息角色 user/assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息Token数 */
    private Integer tokens;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
