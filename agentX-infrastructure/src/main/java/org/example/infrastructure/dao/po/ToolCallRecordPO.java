package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具调用记录持久化对象。
 */
@Data
public class ToolCallRecordPO {

    private String id;
    private String toolId;
    private String methodName;
    private String userId;
    private String sessionId;
    private String argumentsJson;
    private String resultText;
    private Boolean success;
    private String errorMessage;
    private Long latencyMs;
    private LocalDateTime createdAt;
}
