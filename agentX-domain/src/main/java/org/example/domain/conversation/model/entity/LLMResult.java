package org.example.domain.conversation.model.entity;

import org.example.domain.conversation.model.valobj.TokenUsage;

/**
 * 非流式模型调用结果。
 */
public record LLMResult(
        String content,
        TokenUsage usage,
        String finishReason
) {
}
