package org.example.domain.message.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息领域实体
 */
@Data
public class MessageEntity {

    /** 消息唯一ID */
    private String id;

    /** 所属会话ID */
    private String sessionId;

    /** 消息角色：user-用户消息，assistant-AI回复 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息Token数（为后续Token溢出策略准备） */
    private Integer tokens;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 工厂方法：创建用户消息
     */
    public static MessageEntity createUserMessage(String sessionId, String content) {
        return create(sessionId, "user", content, 0);
    }

    /**
     * 工厂方法：创建AI回复消息
     */
    public static MessageEntity createAssistantMessage(String sessionId, String content, Integer tokens) {
        return create(sessionId, "assistant", content, tokens);
    }

    private static MessageEntity create(String sessionId, String role, String content, Integer tokens) {
        MessageEntity message = new MessageEntity();
        message.setId(UUID.randomUUID().toString().replace("-", ""));
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setTokens(tokens != null ? tokens : 0);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
