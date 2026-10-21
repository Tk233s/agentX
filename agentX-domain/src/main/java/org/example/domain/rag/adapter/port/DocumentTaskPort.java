package org.example.domain.rag.adapter.port;

/**
 * 文档后台处理调度端口。
 *
 * 领域层只表达“提交一个处理任务”，具体线程池和排队策略由基础设施适配。
 */
public interface DocumentTaskPort {

    void submit(Runnable task);
}
