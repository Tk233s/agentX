package org.example.domain.session.adapter.repository;

import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.model.valobj.SessionMemory;

import java.util.List;

/**
 * 会话仓储接口（端口）
 */
public interface SessionRepository {

    /**
     * 新增会话
     */
    void save(SessionEntity session);

    /**
     * 更新会话
     */
    void update(SessionEntity session);

    /**
     * 根据ID删除
     */
    int deleteById(String id);

    /**
     * 根据ID查询
     */
    SessionEntity findById(String id);

    /**
     * 根据用户ID查询所有会话（按最后更新时间倒序）
     */
    List<SessionEntity> queryByUserId(String userId);

    /**
     * 原子累加会话已使用Token。
     *
     * @return 影响行数
     */
    int incrementUsedTokens(String id, long tokens);

    /**
     * 以摘要水位做乐观锁更新，避免并发请求用旧摘要覆盖新摘要。
     *
     * @return 影响行数
     */
    int updateMemory(String id, String expectedThroughMessageId, SessionMemory memory);
}
