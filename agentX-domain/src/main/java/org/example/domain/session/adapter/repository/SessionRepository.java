package org.example.domain.session.adapter.repository;

import org.example.domain.session.model.entity.SessionEntity;

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
}
