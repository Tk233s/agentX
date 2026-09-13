package org.example.domain.auth.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户领域实体
 */
@Data
public class UserEntity {

    /** 用户ID */
    private String id;

    /** 用户名 */
    private String username;

    /** 密码哈希 */
    private String passwordHash;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
