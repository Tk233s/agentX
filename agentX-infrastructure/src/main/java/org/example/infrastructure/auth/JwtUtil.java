package org.example.infrastructure.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.domain.auth.adapter.port.TokenPort;
import org.example.domain.auth.model.entity.TokenEntity;
import org.example.domain.auth.model.entity.UserEntity;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT工具
 */
@Component
public class JwtUtil implements TokenPort {

    @Value("${auth.jwt.secret}")
    private String secret;

    @Value("${auth.jwt.expire-minutes:120}")
    private long expireMinutes;

    @Override
    public TokenEntity generate(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expireMinutes * 60);
        String token = JWT.create()
                .withSubject(user.getId())
                .withClaim("username", user.getUsername())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm());
        return new TokenEntity(token, LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
    }

    @Override
    public UserEntity verify(String token) {
        try {
            DecodedJWT decoded = JWT.require(algorithm()).build().verify(token);
            String userId = decoded.getSubject();
            String username = decoded.getClaim("username").asString();
            if (userId == null || userId.isBlank() || username == null || username.isBlank()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "访问令牌无效");
            }
            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setUsername(username);
            return user;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "访问令牌无效或已过期");
        }
    }

    private Algorithm algorithm() {
        if (secret == null || secret.trim().length() < 32) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "JWT密钥不能为空且长度不能少于32字符");
        }
        return Algorithm.HMAC256(secret);
    }
}
