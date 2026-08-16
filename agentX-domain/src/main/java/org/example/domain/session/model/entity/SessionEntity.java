package org.example.domain.session.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话领域实体
 */
@Data
public class SessionEntity {

    /** 会话唯一ID */
    private String id;

    /** 会话标题 */
    private String title;

    /** 关联的智能体ID */
    private String agentId;

    /** 所属用户ID */
    private String userId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间（最后一条消息的时间，用于会话排序） */
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建新会话
     */
    public static SessionEntity createNew(String agentId, String userId, String title) {
        SessionEntity session = new SessionEntity();
        session.setId(UUID.randomUUID().toString().replace("-", ""));
        session.setAgentId(agentId);
        session.setUserId(userId);
        session.setTitle(title != null ? title : "新会话");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return session;
    }
}
