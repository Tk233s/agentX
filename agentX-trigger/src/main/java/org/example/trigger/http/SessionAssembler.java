package org.example.trigger.http;

import org.example.domain.session.model.entity.SessionEntity;
import org.example.trigger.dto.session.SessionReq;
import org.example.trigger.dto.session.SessionRes;

/**
 * 会话 DTO 转换器
 */
public class SessionAssembler {

    /** 入参 -> 领域实体（创建） */
    public static SessionEntity toEntity(SessionReq req) {
        return SessionEntity.createNew(
                req.getAgentId(),
                req.getUserId(),
                req.getTitle());
    }

    /** 领域实体 -> 出参 */
    public static SessionRes toRes(SessionEntity entity) {
        SessionRes res = new SessionRes();
        res.setId(entity.getId());
        res.setTitle(entity.getTitle());
        res.setAgentId(entity.getAgentId());
        res.setUserId(entity.getUserId());
        res.setCreateTime(entity.getCreatedAt());
        res.setUpdateTime(entity.getUpdatedAt());
        return res;
    }
}
