package org.example.trigger.dto.tool;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具调用记录出参。
 */
@Data
public class ToolCallRecordRes {

    private String id;
    private String toolId;
    private String methodName;
    private String sessionId;
    private String argumentsJson;
    private String resultText;
    private boolean success;
    private String errorMessage;
    private long latencyMs;
    private LocalDateTime createTime;
}
