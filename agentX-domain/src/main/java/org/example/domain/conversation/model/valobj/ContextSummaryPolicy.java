package org.example.domain.conversation.model.valobj;

/**
 * 上下文摘要策略。
 */
public record ContextSummaryPolicy(
        boolean enabled,
        double triggerRatio,
        double targetRatio,
        int maxSummaryTokens,
        int minRecentGroups
) {

    public ContextSummaryPolicy {
        if (triggerRatio <= 0 || triggerRatio > 1) {
            throw new IllegalArgumentException("triggerRatio must be between 0 and 1");
        }
        if (targetRatio <= 0 || targetRatio > triggerRatio) {
            throw new IllegalArgumentException("targetRatio must be between 0 and triggerRatio");
        }
        if (maxSummaryTokens <= 0) {
            throw new IllegalArgumentException("maxSummaryTokens must be greater than 0");
        }
        if (minRecentGroups < 1) {
            throw new IllegalArgumentException("minRecentGroups must be greater than 0");
        }
    }

    public static ContextSummaryPolicy disabled() {
        return new ContextSummaryPolicy(false, 0.80, 0.65, 4096, 4);
    }
}
