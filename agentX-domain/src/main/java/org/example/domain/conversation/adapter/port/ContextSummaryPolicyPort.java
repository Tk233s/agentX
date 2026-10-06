package org.example.domain.conversation.adapter.port;

import org.example.domain.conversation.model.valobj.ContextSummaryPolicy;

/**
 * 上下文摘要策略配置端口。
 */
public interface ContextSummaryPolicyPort {

    ContextSummaryPolicy getPolicy();
}
