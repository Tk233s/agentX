package org.example.domain.conversation.model.valobj;

import org.example.domain.message.model.entity.MessageEntity;

import java.util.List;

/**
 * 一次模型调用最终使用的会话上下文。
 */
public record ConversationContext(
        String summary,
        List<MessageEntity> messages
) {

    public ConversationContext {
        summary = summary == null || summary.isBlank() ? null : summary;
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public boolean hasSummary() {
        return summary != null;
    }
}
