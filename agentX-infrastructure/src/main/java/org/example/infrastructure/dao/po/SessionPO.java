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

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
