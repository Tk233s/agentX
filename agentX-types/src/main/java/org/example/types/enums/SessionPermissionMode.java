package org.example.types.enums;

import org.example.types.exception.AppException;

import java.util.Locale;

/**
 * 会话权限档。
 *
 * 能力模式决定"这个 Agent 有哪些工具可用"，权限档决定"这次对话允许做到什么程度"。
 * 两者拆开之后，同一个 Agent 的不同会话可以有不同自由度，改权限也不必回配置页。
 */
public enum SessionPermissionMode {

    /** 只读：不挂载任何会产生副作用的工具，模型看不到写入能力。 */
    READ_ONLY,

    /** 放行：Agent 绑定的工具全部生效。 */
    FULL;

    /**
     * 解析数据库或接口传入的权限值。
     * 空值表示沿用旧行为，按放行处理。
     */
    public static SessionPermissionMode parse(String value) {
        if (value == null || value.isBlank()) {
            return FULL;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "不支持的会话权限档：" + value);
        }
    }
}
