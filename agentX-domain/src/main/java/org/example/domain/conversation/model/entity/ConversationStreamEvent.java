package org.example.domain.conversation.model.entity;

import org.example.domain.conversation.model.valobj.TokenUsage;
import org.example.domain.session.model.valobj.SessionTokenBudget;

/**
 * 对话领域流式事件，HTTP 层负责转换成 SSE。
 */
public record ConversationStreamEvent(
        EventType type,
        String content,
        String messageId,
        String model,
        String provider,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String usageSource,
        String finishReason,
        Long latencyMs,
        Long tokenLimit,
        Long usedTokens,
        Long remainingTokens,
        Boolean limitReached
) {

    public enum EventType {
        START,
        DELTA,
        USAGE,
        DONE,
        ERROR
    }

    public static ConversationStreamEvent start(
            String messageId,
            String model,
            String provider,
            SessionTokenBudget tokenBudget) {
        return new ConversationStreamEvent(
                EventType.START, null, messageId, model, provider,
                null, null, null, null, null, null,
                tokenBudget.limit(), tokenBudget.usedTokens(), tokenBudget.remainingTokens(), tokenBudget.isExhausted());
    }

    public static ConversationStreamEvent delta(String content) {
        return new ConversationStreamEvent(
                EventType.DELTA, content, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null);
    }

    public static ConversationStreamEvent usage(TokenUsage usage) {
        return new ConversationStreamEvent(
                EventType.USAGE, null, null, null, null,
                usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage.source(),
                null, null,
                null, null, null, null);
    }

    public static ConversationStreamEvent done(
            String messageId,
            String finishReason,
            Long latencyMs,
            TokenUsage usage,
            SessionTokenBudget tokenBudget) {
        return new ConversationStreamEvent(
                EventType.DONE, null, messageId, null, null,
                usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage.source(),
                finishReason, latencyMs,
                tokenBudget.limit(), tokenBudget.usedTokens(), tokenBudget.remainingTokens(), tokenBudget.isExhausted());
    }

    public static ConversationStreamEvent error(String message) {
        return new ConversationStreamEvent(
                EventType.ERROR, message, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null);
    }
}
