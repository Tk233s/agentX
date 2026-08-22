package org.example.trigger.dto.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建智能体 HTTP 出参 DTO
 * 只包含前端需要展示的字段，不暴露任何敏感/内部字段。
 */
@Data
public class AgentRes {

    /** 智能体唯一ID */
    private String id;

    /** 智能体名称 */
    private String name;

    /** 智能体头像URL */
    private String avatar;

    /** 智能体描述 */
    private String description;

    /** 智能体系统提示词 */
    private String systemPrompt;

    /** 欢迎消息 */
    private String welcomeMessage;

    /** 服务商 */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 温度参数 */
    private Double temperature;

    /** Top-P参数 */
    private Double topP;

    /** Top-K参数 */
    private Integer topK;

    /** 最大Token数 */
    private Integer maxTokens;

    /** 启用的工具名称列表 */
    private java.util.List<String> tools;

    /** 智能体状态 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
