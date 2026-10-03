package org.example.infrastructure.adapter.repository;

import org.example.domain.session.adapter.repository.SessionRepository;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.infrastructure.dao.ISessionDao;
import org.example.infrastructure.dao.po.SessionPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话仓储实现
 */
@Repository
public class SessionRepositoryImpl implements SessionRepository {

    @Resource
    private ISessionDao sessionDao;

    @Override
    public void save(SessionEntity session) {
        sessionDao.insert(toPO(session));
    }

    @Override
    public void update(SessionEntity session) {
        sessionDao.update(toPO(session));
    }

    @Override
    public int deleteById(String id) {
        return sessionDao.deleteById(id);
    }

    @Override
    public SessionEntity findById(String id) {
        return toEntity(sessionDao.queryById(id));
    }

    @Override
    public List<SessionEntity> queryByUserId(String userId) {
        List<SessionPO> pos = sessionDao.queryByUserId(userId);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        return pos.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public int incrementUsedTokens(String id, long tokens) {
        return sessionDao.incrementUsedTokens(id, tokens);
    }

    private SessionPO toPO(SessionEntity entity) {
        SessionPO po = new SessionPO();
        po.setId(entity.getId());
        po.setTitle(entity.getTitle());
        po.setAgentId(entity.getAgentId());
        po.setUserId(entity.getUserId());
        po.setTokenLimit(entity.getTokenLimit());
        po.setUsedTokens(entity.getUsedTokens());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }

    private SessionEntity toEntity(SessionPO po) {
        if (po == null) {
            return null;
        }
        SessionEntity entity = new SessionEntity();
        entity.setId(po.getId());
        entity.setTitle(po.getTitle());
        entity.setAgentId(po.getAgentId());
        entity.setUserId(po.getUserId());
        entity.setTokenLimit(po.getTokenLimit());
        entity.setUsedTokens(po.getUsedTokens());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }
}
