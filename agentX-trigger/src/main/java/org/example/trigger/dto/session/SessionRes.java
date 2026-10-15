package org.example.trigger.dto.session;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话 HTTP 出参 DTO
 */
@Data
public class SessionRes {

    private String id;

    private String title;

    private String agentId;

    private String userId;

    /** 会话累计Token上限；null表示不限制 */
    private Long tokenLimit;

    /** 会话累计已使用Token */
    private Long usedTokens;

    /** 会话权限档：READ_ONLY 只读、FULL 放行 */
    private String permissionMode;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
