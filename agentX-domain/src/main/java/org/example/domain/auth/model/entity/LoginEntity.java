package org.example.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录结果实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEntity {

    /** 用户ID */
    private String userId;

    /** 用户名 */
    private String username;

    /** 访问令牌 */
    private String token;

    /** 过期时间 */
    private LocalDateTime expiresAt;
}
