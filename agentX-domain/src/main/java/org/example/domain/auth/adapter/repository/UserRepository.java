package org.example.domain.auth.adapter.repository;

import org.example.domain.auth.model.entity.UserEntity;

/**
 * 用户仓储接口（端口）
 */
public interface UserRepository {

    /**
     * 根据用户名查询用户
     */
    UserEntity findByUsername(String username);
}
