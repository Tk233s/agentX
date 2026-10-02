package org.example.domain.message.model.entity;

import org.example.domain.conversation.model.valobj.TokenUsage;
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

    /** 输入Token数 */
    private Integer promptTokens;

    /** 输出Token数 */
    private Integer completionTokens;

    /** 总Token数 */
    private Integer totalTokens;

    /** 使用的模型 */
    private String model;

    /** 模型服务商 */
    private String provider;

    /** 流结束原因：stop/length/cancelled/error */
    private String finishReason;

    /** 本次生成耗时（毫秒） */
    private Long latencyMs;

    /** Token来源：provider/estimated */
    private String usageSource;

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

    /**
     * 工厂方法：创建带完整元数据的AI回复消息。
     */
    public static MessageEntity createAssistantMessage(
            String id,
            String sessionId,
            String content,
            TokenUsage usage,
            String model,
            String provider,
            String finishReason,
            Long latencyMs) {
        MessageEntity message = create(sessionId, "assistant", content, usage == null ? 0 : usage.totalTokens());
        if (id != null && !id.isBlank()) {
            message.setId(id);
        }
        if (usage != null) {
            message.setPromptTokens(usage.promptTokens());
            message.setCompletionTokens(usage.completionTokens());
            message.setTotalTokens(usage.totalTokens());
            message.setTokens(usage.totalTokens());
            message.setUsageSource(usage.source());
        }
        message.setModel(model);
        message.setProvider(provider);
        message.setFinishReason(finishReason);
        message.setLatencyMs(latencyMs);
        return message;
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
