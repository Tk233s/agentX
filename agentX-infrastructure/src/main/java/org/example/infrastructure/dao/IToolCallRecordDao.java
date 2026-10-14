package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.dao.po.ToolCallRecordPO;

import java.util.List;

/**
 * 工具调用记录 DAO。
 */
@Mapper
public interface IToolCallRecordDao {

    void insert(ToolCallRecordPO recordPO);

    List<ToolCallRecordPO> queryBySessionIdAndUserId(
            @Param("sessionId") String sessionId,
            @Param("userId") String userId,
            @Param("limit") int limit);
}
