package org.example.domain.conversation.model.entity;

import org.example.domain.conversation.model.valobj.TokenUsage;

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
        Long latencyMs
) {

    public enum EventType {
        START,
        DELTA,
        USAGE,
        DONE,
        ERROR
    }

    public static ConversationStreamEvent start(String messageId, String model, String provider) {
        return new ConversationStreamEvent(
                EventType.START, null, messageId, model, provider,
                null, null, null, null, null, null);
    }

    public static ConversationStreamEvent delta(String content) {
        return new ConversationStreamEvent(
                EventType.DELTA, content, null, null, null,
                null, null, null, null, null, null);
    }

    public static ConversationStreamEvent usage(TokenUsage usage) {
        return new ConversationStreamEvent(
                EventType.USAGE, null, null, null, null,
                usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage.source(),
                null, null);
    }

    public static ConversationStreamEvent done(
            String messageId,
            String finishReason,
            Long latencyMs,
            TokenUsage usage) {
        return new ConversationStreamEvent(
                EventType.DONE, null, messageId, null, null,
                usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage.source(),
                finishReason, latencyMs);
    }

    public static ConversationStreamEvent error(String message) {
        return new ConversationStreamEvent(
                EventType.ERROR, message, null, null, null,
                null, null, null, null, null, null);
    }
}
