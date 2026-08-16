package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.dao.po.SessionPO;

import java.util.List;

/**
 * 会话 DAO 接口
 */
@Mapper
public interface ISessionDao {

    void insert(SessionPO sessionPO);

    int update(SessionPO sessionPO);

    int deleteById(String id);

    SessionPO queryById(String id);

    List<SessionPO> queryByUserId(String userId);
}
