package org.example.domain.conversation.model.valobj;

/**
 * 模型上下文窗口配置。
 */
public record ContextWindow(
        int maxWindowTokens,
        int outputReserveTokens
) {

    public ContextWindow {
        if (maxWindowTokens <= 0) {
            throw new IllegalArgumentException("maxWindowTokens must be greater than 0");
        }
        if (outputReserveTokens < 0 || outputReserveTokens >= maxWindowTokens) {
            throw new IllegalArgumentException("outputReserveTokens must be between 0 and maxWindowTokens");
        }
    }

    /**
     * 可用于输入消息的最大 Token 数。
     */
    public int maxInputTokens() {
        return maxWindowTokens - outputReserveTokens;
    }
}
