package org.example.trigger.dto.session;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 会话 HTTP 入参 DTO
 */
@Data
public class SessionReq {

    /** 会话ID（创建时忽略，更新时必填） */
    private String id;

    /** 会话标题 */
    private String title;

    /** 关联的智能体ID（创建时必填） */
    @NotBlank(message = "智能体ID不能为空")
    private String agentId;

    /** 会话累计Token上限；为空表示不限制 */
    @Min(value = 1, message = "会话Token上限必须大于0")
    private Long tokenLimit;
}
