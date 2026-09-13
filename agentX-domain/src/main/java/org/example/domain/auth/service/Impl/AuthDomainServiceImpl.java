package org.example.domain.auth.service.Impl;

import org.example.domain.auth.adapter.port.TokenPort;
import org.example.domain.auth.adapter.repository.UserRepository;
import org.example.domain.auth.model.entity.LoginEntity;
import org.example.domain.auth.model.entity.TokenEntity;
import org.example.domain.auth.model.entity.UserEntity;
import org.example.domain.auth.service.IAuthDomainService;
import org.example.domain.auth.util.PasswordHasher;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 认证领域服务实现
 */
@Service
public class AuthDomainServiceImpl implements IAuthDomainService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private TokenPort tokenPort;

    @Override
    public LoginEntity login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null || !PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户名或密码错误");
        }
        TokenEntity token = tokenPort.generate(user);
        return new LoginEntity(user.getId(), user.getUsername(), token.getToken(), token.getExpiresAt());
    }
}
