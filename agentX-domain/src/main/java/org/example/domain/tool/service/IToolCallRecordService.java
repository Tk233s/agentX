package org.example.domain.tool.service;

import org.example.domain.tool.model.entity.ToolCallRecordEntity;

import java.util.List;

/**
 * 工具调用记录查询服务。
 */
public interface IToolCallRecordService {

    List<ToolCallRecordEntity> listCallRecords(
            String sessionId,
            String userId,
            Integer limit);
}
