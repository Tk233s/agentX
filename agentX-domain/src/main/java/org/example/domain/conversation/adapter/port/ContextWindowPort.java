package org.example.domain.conversation.adapter.port;

import org.example.domain.conversation.model.valobj.ContextWindow;

/**
 * 上下文窗口配置端口。
 */
public interface ContextWindowPort {

    ContextWindow getContextWindow(String model);
}
