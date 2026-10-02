package org.example.domain.conversation.model.valobj;

/**
 * 一次模型调用产生的 Token 使用量。
 */
public record TokenUsage(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String source
) {

    public static final String SOURCE_PROVIDER = "provider";
    public static final String SOURCE_ESTIMATED = "estimated";

    public static TokenUsage provider(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return normalized(promptTokens, completionTokens, totalTokens, SOURCE_PROVIDER);
    }

    public static TokenUsage estimated(Integer promptTokens, Integer completionTokens) {
        return normalized(promptTokens, completionTokens, null, SOURCE_ESTIMATED);
    }

    public boolean hasUsage() {
        return totalTokens != null && totalTokens > 0;
    }

    private static TokenUsage normalized(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String source) {
        Integer normalizedTotal = totalTokens;
        if (normalizedTotal == null && (promptTokens != null || completionTokens != null)) {
            normalizedTotal = value(promptTokens) + value(completionTokens);
        }
        return new TokenUsage(promptTokens, completionTokens, normalizedTotal, source);
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
