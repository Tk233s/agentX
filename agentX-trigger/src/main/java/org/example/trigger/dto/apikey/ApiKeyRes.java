package org.example.trigger.dto.apikey;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API密钥 HTTP 出参 DTO
 * 出于安全考虑，列表/详情接口中 apiKey 会脱敏展示（仅显示后4位）。
 */
@Data
public class ApiKeyRes {

    /** 主键ID */
    private String id;

    /** 服务商 */
    private String provider;

    /** API密钥（脱敏：sk-****xxxx） */
    private String apiKey;

    /** 自定义API地址 */
    private String baseUrl;

    /** 是否启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
