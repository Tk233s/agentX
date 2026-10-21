package org.example.infrastructure.rag;

import org.example.domain.rag.adapter.port.DocumentTaskPort;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 复用应用现有线程池执行文档后台处理。
 */
@Component
public class ThreadPoolDocumentTaskAdapter implements DocumentTaskPort {

    private final ThreadPoolExecutor threadPoolExecutor;

    public ThreadPoolDocumentTaskAdapter(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void submit(Runnable task) {
        threadPoolExecutor.execute(task);
    }
}
