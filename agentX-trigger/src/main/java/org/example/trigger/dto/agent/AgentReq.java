package org.example.trigger.dto.agent;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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

    /** 服务商 openai/anthropic */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 温度参数 0-2 */
    @Min(value = 0, message = "temperature最小值为0")
    @Max(value = 2, message = "temperature最大值为2")
    private Double temperature;

    /** Top-P参数 0-1 */
    @Min(value = 0, message = "topP最小值为0")
    @Max(value = 1, message = "topP最大值为1")
    private Double topP;

    /** Top-K参数 */
    private Integer topK;

    /** 最大Token数 */
    @Min(value = 1, message = "maxTokens最小值为1")
    private Integer maxTokens;

    /** 创建者用户ID；TODO 临时占位，后续接入登录态（ThreadLocal/上下文）后移除 */
    private String userId;
}
