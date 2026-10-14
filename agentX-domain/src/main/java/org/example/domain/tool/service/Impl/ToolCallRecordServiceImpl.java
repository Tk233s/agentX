package org.example.domain.tool.service.Impl;

import org.example.domain.tool.adapter.repository.ToolCallRecordRepository;
import org.example.domain.tool.model.entity.ToolCallRecordEntity;
import org.example.domain.tool.service.IToolCallRecordService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 工具调用记录查询服务实现。
 */
@Service
public class ToolCallRecordServiceImpl implements IToolCallRecordService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    @Resource
    private ToolCallRecordRepository toolCallRecordRepository;

    @Override
    public List<ToolCallRecordEntity> listCallRecords(
            String sessionId,
            String userId,
            Integer limit) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话ID不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new AppException(ResponseCode.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }

        int normalizedLimit = limit == null || limit < 1
                ? DEFAULT_LIMIT
                : Math.min(limit, MAX_LIMIT);
        return toolCallRecordRepository.queryBySessionIdAndUserId(
                sessionId,
                userId,
                normalizedLimit);
    }
}
