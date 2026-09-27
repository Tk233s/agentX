package org.example.trigger.dto.apikey;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * API密钥 HTTP 入参 DTO（创建/更新共用）
 * id 采用校验分组：创建时忽略，更新时必填。
 */
@Data
public class ApiKeyReq {

    /** 主键ID；创建时忽略，更新时必传 */
    @NotBlank(groups = ApiKeyUpdateGroup.class, message = "API密钥ID不能为空")
    private String id;

    /** 服务商 openai/anthropic */
    @NotBlank(message = "服务商不能为空")
    private String provider;

    /** API密钥 */
    @NotBlank(message = "API密钥不能为空")
    private String apiKey;

    /** 自定义API地址（代理/中转） */
    private String baseUrl;

}
