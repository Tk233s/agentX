package org.example.domain.session.model.entity;

import lombok.Data;
import org.example.domain.session.model.valobj.SessionMemory;
import org.example.domain.session.model.valobj.SessionTokenBudget;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

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

    /** 会话累计Token上限；null表示不限制 */
    private Long tokenLimit;

    /** 会话累计已使用Token */
    private Long usedTokens;

    /** 较早对话的压缩摘要 */
    private String contextSummary;

    /** 摘要已经覆盖到的最后一条消息ID */
    private String summaryThroughMessageId;

    /** 摘要最后更新时间 */
    private LocalDateTime summaryUpdatedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间（最后一条消息的时间，用于会话排序） */
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建新会话
     */
    public static SessionEntity createNew(String agentId, String userId, String title, Long tokenLimit) {
        if (tokenLimit != null && tokenLimit <= 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话Token上限必须大于0");
        }
        SessionEntity session = new SessionEntity();
        session.setId(UUID.randomUUID().toString().replace("-", ""));
        session.setAgentId(agentId);
        session.setUserId(userId);
        session.setTitle(title != null ? title : "新会话");
        session.setTokenLimit(tokenLimit);
        session.setUsedTokens(0L);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return session;
    }

    public SessionTokenBudget tokenBudget() {
        return SessionTokenBudget.of(tokenLimit, usedTokens);
    }

    public SessionMemory memory() {
        return new SessionMemory(contextSummary, summaryThroughMessageId, summaryUpdatedAt);
    }

    public void updateMemory(SessionMemory memory) {
        SessionMemory target = memory == null ? SessionMemory.empty() : memory;
        this.contextSummary = target.summary();
        this.summaryThroughMessageId = target.summarizedThroughMessageId();
        this.summaryUpdatedAt = target.updatedAt();
    }
}
