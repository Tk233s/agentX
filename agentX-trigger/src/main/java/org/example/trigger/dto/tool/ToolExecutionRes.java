package org.example.trigger.dto.tool;

import lombok.Data;

/**
 * 工具执行结果出参。
 */
@Data
public class ToolExecutionRes {

    private String toolId;
    private String methodName;
    private boolean success;
    private String resultText;
    private String errorMessage;
    private long latencyMs;
}
