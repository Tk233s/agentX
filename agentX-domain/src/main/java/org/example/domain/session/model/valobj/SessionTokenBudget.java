package org.example.domain.session.model.valobj;

/**
 * 会话 Token 累计预算。
 *
 * <p>limit 为 null 表示不限制；usedTokens 始终表示已经完成的模型调用所消耗的 Token 总量。</p>
 */
public record SessionTokenBudget(Long limit, long usedTokens) {

    public SessionTokenBudget {
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("会话Token上限必须大于0");
        }
        usedTokens = Math.max(usedTokens, 0L);
    }

    public static SessionTokenBudget of(Long limit, Long usedTokens) {
        return new SessionTokenBudget(limit, usedTokens == null ? 0L : usedTokens);
    }

    public boolean isUnlimited() {
        return limit == null;
    }

    public boolean isExhausted() {
        return !isUnlimited() && usedTokens >= limit;
    }

    public Long remainingTokens() {
        if (isUnlimited()) {
            return null;
        }
        return Math.max(limit - usedTokens, 0L);
    }
}
