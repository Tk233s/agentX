package org.example.domain.tool.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工具调用审计记录。
 */
@Data
public class ToolCallRecordEntity {

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

    public static ToolCallRecordEntity create(
            String toolId,
            String methodName,
            String userId,
            String sessionId,
            String argumentsJson,
            String resultText,
            boolean success,
            String errorMessage,
            long latencyMs) {
        ToolCallRecordEntity record = new ToolCallRecordEntity();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setToolId(toolId);
        record.setMethodName(methodName);
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setArgumentsJson(argumentsJson);
        record.setResultText(resultText);
        record.setSuccess(success);
        record.setErrorMessage(errorMessage);
        record.setLatencyMs(latencyMs);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }
}
