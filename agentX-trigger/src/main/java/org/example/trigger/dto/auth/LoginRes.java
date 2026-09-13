package org.example.trigger.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录HTTP出参DTO
 */
@Data
public class LoginRes {

    /** 用户ID */
    private String userId;

    /** 用户名 */
    private String username;

    /** 访问令牌 */
    private String token;

    /** 过期时间 */
    private LocalDateTime expiresAt;
}
