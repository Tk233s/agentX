package org.example.rag;

import org.example.domain.rag.adapter.repository.KnowledgeDocumentRepository;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.service.Impl.RagDomainServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 启动时回收中断文档任务。
 */
public class DocumentRecoveryTest {

    @Test
    public void marksStaleProcessingDocumentAsFailed() {
        KnowledgeDocumentRepository repository = mock(KnowledgeDocumentRepository.class);
        KnowledgeDocumentEntity stale = document("doc-1", KnowledgeDocumentEntity.STATUS_PROCESSING);

        when(repository.queryByStatusBefore(eq(KnowledgeDocumentEntity.STATUS_PROCESSING), any(LocalDateTime.class)))
                .thenReturn(List.of(stale));

        int recovered = service(repository).recoverInterruptedDocuments(10);

        assertEquals(1, recovered);
        assertEquals(KnowledgeDocumentEntity.STATUS_FAILED, stale.getStatus());
        assertTrue(stale.getErrorMessage().contains("重试"));
        verify(repository).updateProcessResult(stale);
    }

    @Test
    public void leavesReadyDocumentsUntouched() {
        KnowledgeDocumentRepository repository = mock(KnowledgeDocumentRepository.class);
        when(repository.queryByStatusBefore(eq(KnowledgeDocumentEntity.STATUS_PROCESSING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int recovered = service(repository).recoverInterruptedDocuments(10);

        assertEquals(0, recovered);
        verify(repository, never()).updateProcessResult(any(KnowledgeDocumentEntity.class));
    }

    @Test
    public void cutoffFollowsConfiguredStaleMinutes() {
        KnowledgeDocumentRepository repository = mock(KnowledgeDocumentRepository.class);
        when(repository.queryByStatusBefore(eq(KnowledgeDocumentEntity.STATUS_PROCESSING), any(LocalDateTime.class)))
                .thenReturn(List.of());
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);

        service(repository).recoverInterruptedDocuments(10);

        verify(repository).queryByStatusBefore(eq(KnowledgeDocumentEntity.STATUS_PROCESSING), cutoff.capture());
        long actualMinutes = Duration.between(cutoff.getValue(), LocalDateTime.now()).toMinutes();
        assertTrue("时间窗口应接近 10 分钟，实际 " + actualMinutes, actualMinutes >= 9 && actualMinutes <= 10);
    }

    private RagDomainServiceImpl service(KnowledgeDocumentRepository repository) {
        RagDomainServiceImpl service = new RagDomainServiceImpl();
        ReflectionTestUtils.setField(service, "knowledgeDocumentRepository", repository);
        return service;
    }

    private KnowledgeDocumentEntity document(String id, String status) {
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setId(id);
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now().minusHours(2));
        return entity;
    }
}
