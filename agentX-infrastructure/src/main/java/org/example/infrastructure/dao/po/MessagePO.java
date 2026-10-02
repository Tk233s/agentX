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

    /** 流结束原因 */
    private String finishReason;

    /** 本次生成耗时（毫秒） */
    private Long latencyMs;

    /** Token来源 */
    private String usageSource;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
