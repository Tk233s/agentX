package org.example.trigger.dto.conversation;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 对话 HTTP 入参 DTO
 */
@Data
public class ConversationReq {

    /** 会话ID */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /** 用户ID；TODO 临时占位，后续接入登录态后移除 */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /** 用户消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
