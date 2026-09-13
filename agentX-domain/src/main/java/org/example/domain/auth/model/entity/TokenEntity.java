package org.example.domain.auth.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 访问令牌实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenEntity {

    /** 访问令牌 */
    private String token;

    /** 过期时间 */
    private LocalDateTime expiresAt;
}
