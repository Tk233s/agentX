package org.example.initializer;

import org.example.domain.rag.service.IRagDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时回收被中断的文档处理任务。
 *
 * <p>文档的后台处理跑在内存线程池上，进程重启或崩溃不会把数据库里的 PROCESSING 状态带走，
 * 而前端只允许对 FAILED 文档发起重试，于是这类文档会永远停在「处理中」，
 * 用户既看不到进度也点不动重试。启动时统一把它们改判为 FAILED，把重试入口还回去。</p>
 *
 * <p>这里刻意吞掉异常：PostgreSQL 比应用晚就绪、或暂时连不上时，
 * 回收失败不应该连累整个服务启动。</p>
 */
@Component
public class DocumentRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentRecoveryRunner.class);

    /** 静默超过这个时长仍处于 PROCESSING，就认为处理它的线程已经不在了。 */
    private static final int STALE_MINUTES = 10;

    private final IRagDomainService ragDomainService;

    public DocumentRecoveryRunner(IRagDomainService ragDomainService) {
        this.ragDomainService = ragDomainService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int recovered = ragDomainService.recoverInterruptedDocuments(STALE_MINUTES);
            if (recovered > 0) {
                log.info("启动回收完成，{} 条中断的文档已置为失败状态", recovered);
            }
        } catch (RuntimeException e) {
            log.warn("启动回收中断文档失败，跳过本次回收：{}", e.getMessage());
        }
    }
}
