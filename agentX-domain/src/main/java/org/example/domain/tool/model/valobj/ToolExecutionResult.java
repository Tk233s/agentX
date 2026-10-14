package org.example.domain.tool.model.valobj;

/**
 * 手动执行工具后的标准化结果。
 */
public record ToolExecutionResult(
        String toolId,
        String methodName,
        boolean success,
        String resultText,
        String errorMessage,
        long latencyMs) {
}
