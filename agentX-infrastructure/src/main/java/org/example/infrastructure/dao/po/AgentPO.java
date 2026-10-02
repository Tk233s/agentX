package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体持久化对象
 */
@Data
public class AgentPO {

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

    /** 服务商 openai/anthropic */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 最大Token数 */
    private Integer maxTokens;

    /** 工具名称列表（JSON 数组字符串）：如 ["weather", "file"] */
    private String toolsJson;

    /** 智能体状态：1-启用，0-禁用 */
    private Boolean enabled;

    /** 创建者用户ID */
    private String userId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
