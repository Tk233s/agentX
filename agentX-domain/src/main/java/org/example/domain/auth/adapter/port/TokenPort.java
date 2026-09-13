package org.example.domain.auth.adapter.port;

import org.example.domain.auth.model.entity.TokenEntity;
import org.example.domain.auth.model.entity.UserEntity;

/**
 * 令牌端口
 */
public interface TokenPort {

    /**
     * 生成访问令牌
     */
    TokenEntity generate(UserEntity user);

    /**
     * 校验访问令牌
     */
    UserEntity verify(String token);
}
