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

    /** 密钥名称，用于区分同一协议下的多个账号或中转地址 */
    @NotBlank(message = "密钥名称不能为空")
    private String name;

    /** 接口协议 openai/anthropic */
    @NotBlank(message = "接口协议不能为空")
    private String provider;

    /** API密钥；更新时留空表示保留原密钥 */
    private String apiKey;

    /** 自定义API地址（代理/中转） */
    private String baseUrl;

    /** 是否启用；创建时为空则默认启用 */
    private Boolean enabled;

}
