package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.dao.po.UserPO;

/**
 * 用户DAO接口
 */
@Mapper
public interface IUserDao {

    /**
     * 根据用户名查询用户
     */
    UserPO queryByUsername(String username);
}
