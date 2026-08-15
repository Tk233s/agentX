package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API密钥持久化对象
 */
@Data
public class ApiKeyPO {

    /** 主键ID */
    private String id;

    /** 所属用户ID */
    private String userId;

    /** 服务商 openai/anthropic */
    private String provider;

    /** API密钥 */
    private String apiKey;

    /** 自定义API地址（代理/中转） */
    private String baseUrl;

    /** 是否启用 0-禁用 1-启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
