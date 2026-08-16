package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话持久化对象
 */
@Data
public class SessionPO {

    /** 会话ID */
    private String id;

    /** 会话标题 */
    private String title;

    /** 关联的智能体ID */
    private String agentId;

    /** 所属用户ID */
    private String userId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
