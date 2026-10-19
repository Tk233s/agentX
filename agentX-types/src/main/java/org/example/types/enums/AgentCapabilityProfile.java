package org.example.types.enums;

import org.example.types.exception.AppException;

import java.util.Locale;

/**
 * Agent 能力模式。
 *
 * 工具由运行时根据模式提供，Agent 不再需要逐项绑定普通工具。
 */
public enum AgentCapabilityProfile {

    /** 普通对话，不默认启用工具。 */
    CHAT,

    /** 代码工作区只读模式。 */
    CODE_READONLY,

    /** 代码工作区模式。 */
    CODE,

    /** 自定义模式，显式工具列表仍然生效。 */
    CUSTOM;

    /**
     * 解析数据库或接口传入的模式值。
     * 空值表示沿用旧行为，按自定义模式处理。
     */
    public static AgentCapabilityProfile parse(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOM;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "不支持的 Agent 能力模式：" + value);
        }
    }
}
