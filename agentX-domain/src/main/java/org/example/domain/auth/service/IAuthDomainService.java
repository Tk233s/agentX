package org.example.domain.auth.service;

import org.example.domain.auth.model.entity.LoginEntity;

/**
 * 认证领域服务接口
 */
public interface IAuthDomainService {

    /**
     * 用户登录
     */
    LoginEntity login(String username, String password);
}
