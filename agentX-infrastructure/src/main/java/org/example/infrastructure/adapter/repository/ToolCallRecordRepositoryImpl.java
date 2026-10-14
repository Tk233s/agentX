package org.example.infrastructure.adapter.repository;

import org.example.domain.tool.adapter.repository.ToolCallRecordRepository;
import org.example.domain.tool.model.entity.ToolCallRecordEntity;
import org.example.infrastructure.dao.IToolCallRecordDao;
import org.example.infrastructure.dao.po.ToolCallRecordPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 工具调用记录仓储实现。
 */
@Repository
public class ToolCallRecordRepositoryImpl implements ToolCallRecordRepository {

    @Resource
    private IToolCallRecordDao toolCallRecordDao;

    @Override
    public void save(ToolCallRecordEntity record) {
        toolCallRecordDao.insert(toPO(record));
    }

    @Override
    public List<ToolCallRecordEntity> queryBySessionIdAndUserId(
            String sessionId,
            String userId,
            int limit) {
        List<ToolCallRecordPO> records = toolCallRecordDao.queryBySessionIdAndUserId(
                sessionId,
                userId,
                limit);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::toEntity).toList();
    }

    private ToolCallRecordPO toPO(ToolCallRecordEntity record) {
        ToolCallRecordPO po = new ToolCallRecordPO();
        po.setId(record.getId());
        po.setToolId(record.getToolId());
        po.setMethodName(record.getMethodName());
        po.setUserId(record.getUserId());
        po.setSessionId(record.getSessionId());
        po.setArgumentsJson(record.getArgumentsJson());
        po.setResultText(record.getResultText());
        po.setSuccess(record.getSuccess());
        po.setErrorMessage(record.getErrorMessage());
        po.setLatencyMs(record.getLatencyMs());
        po.setCreatedAt(record.getCreatedAt());
        return po;
    }

    private ToolCallRecordEntity toEntity(ToolCallRecordPO po) {
        if (po == null) {
            return null;
        }
        ToolCallRecordEntity entity = new ToolCallRecordEntity();
        entity.setId(po.getId());
        entity.setToolId(po.getToolId());
        entity.setMethodName(po.getMethodName());
        entity.setUserId(po.getUserId());
        entity.setSessionId(po.getSessionId());
        entity.setArgumentsJson(po.getArgumentsJson());
        entity.setResultText(po.getResultText());
        entity.setSuccess(po.getSuccess());
        entity.setErrorMessage(po.getErrorMessage());
        entity.setLatencyMs(po.getLatencyMs());
        entity.setCreatedAt(po.getCreatedAt());
        return entity;
    }
}
