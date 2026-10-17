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

    /** 绑定的API密钥ID */
    private String apiKeyId;

    /** 模型ID */
    private String modelId;

    /** 能力模式 */
    private String capabilityProfile;

    /** 高级工具覆盖列表 */
    private java.util.List<String> tools;

    /** 绑定的知识库ID列表 */
    private java.util.List<String> knowledgeBaseIds;

    /** 智能体状态 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
