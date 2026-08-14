package org.example.trigger.dto.agent;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 智能体 HTTP 入参 DTO（创建/更新共用）
 * 属于入站适配器（trigger）的协议契约，只包含前端能提供/该提供的输入字段。
 * id 采用校验分组：创建走 Default 组（忽略 id），更新走 UpdateGroup 组（id 必填）。
 */
@Data
public class AgentReq {

    /** 智能体唯一ID；创建时忽略（由领域层自动生成），更新时必传（UpdateGroup 分组校验） */
    @NotBlank(groups = UpdateGroup.class, message = "智能体ID不能为空")
    private String id;

    /** 智能体名称 */
    @NotBlank(message = "智能体名称不能为空")
    private String name;

    /** 智能体头像URL */
    private String avatar;

    /** 智能体描述 */
    private String description;

    /** 智能体系统提示词 */
    private String systemPrompt;

    /** 欢迎消息 */
    private String welcomeMessage;

    /** 创建者用户ID；TODO 临时占位，后续接入登录态（ThreadLocal/上下文）后移除 */
    private String userId;
}
