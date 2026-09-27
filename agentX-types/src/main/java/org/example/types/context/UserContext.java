package org.example.types.context;

import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

/**
 * 当前登录用户上下文。
 * 仅由认证过滤器写入，业务层通过参数显式接收用户ID，避免领域层依赖Web线程状态。
 */
public final class UserContext {

    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUserId(String userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static String getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 获取当前登录用户ID；认证上下文缺失时按未登录处理。
     */
    public static String requireCurrentUserId() {
        String userId = getCurrentUserId();
        if (userId == null || userId.isBlank()) {
            throw new AppException(ResponseCode.UNAUTHORIZED.getCode(), ResponseCode.UNAUTHORIZED.getInfo());
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
