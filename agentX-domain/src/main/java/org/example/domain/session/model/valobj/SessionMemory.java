package org.example.domain.session.model.valobj;

import java.time.LocalDateTime;

/**
 * 会话长期记忆。
 *
 * <p>summary 保存较早对话的压缩结果，summarizedThroughMessageId 标记摘要已经覆盖到的最后一条消息。</p>
 */
public record SessionMemory(
        String summary,
        String summarizedThroughMessageId,
        LocalDateTime updatedAt
) {

    public SessionMemory {
        summary = summary == null || summary.isBlank() ? null : summary.trim();
        summarizedThroughMessageId = summarizedThroughMessageId == null || summarizedThroughMessageId.isBlank()
                ? null
                : summarizedThroughMessageId;
        if ((summary == null) != (summarizedThroughMessageId == null)) {
            throw new IllegalArgumentException("摘要和水位必须同时存在或同时为空");
        }
    }

    public static SessionMemory empty() {
        return new SessionMemory(null, null, null);
    }

    public boolean hasSummary() {
        return summary != null;
    }
}
