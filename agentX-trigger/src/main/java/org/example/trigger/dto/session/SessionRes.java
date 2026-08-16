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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
