package org.example.domain.conversation.model.entity;

import org.example.domain.conversation.model.valobj.TokenUsage;

/**
 * 模型流式返回的单个分片。
 */
public record LLMStreamChunk(
        String content,
        TokenUsage usage,
        String finishReason
) {

    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
}
