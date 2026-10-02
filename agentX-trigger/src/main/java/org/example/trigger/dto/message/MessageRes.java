package org.example.trigger.dto.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息 HTTP 出参 DTO
 */
@Data
public class MessageRes {

    private String id;

    private String sessionId;

    /** 消息角色：user/assistant */
    private String role;

    private String content;

    private Integer tokens;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private String model;

    private String provider;

    private String finishReason;

    private Long latencyMs;

    private String usageSource;

    private LocalDateTime createTime;
}
