package org.example.domain.tool.adapter.repository;

import org.example.domain.tool.model.entity.ToolCallRecordEntity;

import java.util.List;

/**
 * 工具调用记录仓储端口。
 */
public interface ToolCallRecordRepository {

    void save(ToolCallRecordEntity record);

    List<ToolCallRecordEntity> queryBySessionIdAndUserId(
            String sessionId,
            String userId,
            int limit);
}
