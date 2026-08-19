package org.example.trigger.dto.agent;

import jakarta.validation.groups.Default;

/**
 * 更新操作的校验分组
 * 继承 Default 组：更新时除本组约束（如 ID 必填）外，同时校验默认组约束（如名称必填）。
 * 用于 AgentReq 在创建/更新共用时，区分 id 字段的校验时机。
 */
public interface UpdateGroup extends Default {
}
