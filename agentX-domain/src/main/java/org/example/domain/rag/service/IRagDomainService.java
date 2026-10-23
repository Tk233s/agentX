package org.example.domain.rag.service;

import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.model.valobj.DocumentIngestResult;

import java.util.List;

/**
 * 知识库领域服务接口。
 *
 * 覆盖三件事：知识库本身的增删查、文档入库（解析-分片-向量化-落库）、检索。
 */
public interface IRagDomainService {

    /** 检索测试接口未显式传 TopK 时使用的默认值。 */
    int DEFAULT_TOP_K = KnowledgeBaseEntity.DEFAULT_TOP_K;

    KnowledgeBaseEntity createKnowledgeBase(KnowledgeBaseEntity knowledgeBase);

    List<KnowledgeBaseEntity> listKnowledgeBases(String userId);

    KnowledgeBaseEntity getKnowledgeBase(String id, String userId);

    /** 更新知识库级别的对话检索阈值和 TopK。 */
    KnowledgeBaseEntity updateRetrievalConfig(
            String id,
            String userId,
            Double minScore,
            Integer topK);

    /** 删除知识库，连同其文档、分片和落盘文件一起清理。 */
    void deleteKnowledgeBase(String id, String userId);

    /**
     * 上传文档并提交后台处理。
     *
     * 文件落盘和 PENDING 记录创建完成后立即返回；解析、分片、向量化由后台任务继续执行。
     * 相同知识库内内容相同的文件会直接返回已有记录。
     */
    DocumentIngestResult ingestDocument(
            String knowledgeBaseId,
            String userId,
            String fileName,
            byte[] content);

    /** 查询单个文档并校验归属。 */
    KnowledgeDocumentEntity getDocument(String documentId, String userId);

    List<KnowledgeDocumentEntity> listDocuments(String knowledgeBaseId, String userId);

    /** 删除文档，连带它的分片和落盘文件。 */
    void deleteDocument(String documentId, String userId);

    /** 后台处理已落盘的文档，只由调度适配器调用。 */
    void processStoredDocument(String documentId, String userId);

    /** 将失败文档重置并重新提交后台处理。 */
    KnowledgeDocumentEntity retryDocument(String documentId, String userId);

    /**
     * 回收被进程中断留下的处理中文档。
     *
     * <p>后台处理是线程池上的内存任务，进程重启或崩溃后数据库里的 PROCESSING 状态不会自己消失，
     * 而前端又只允许对 FAILED 文档发起重试，用户会看到一个永远转圈、也点不动重试的文档。
     * 启动时把这些长时间没有更新的 PROCESSING 文档改判为 FAILED，让重试入口重新可用。</p>
     *
     * @param staleMinutes 判定为中断的最短静默时长（分钟）
     * @return 被回收的文档条数
     */
    int recoverInterruptedDocuments(int staleMinutes);

    /** 使用向量召回、关键词召回和 RRF 融合检索最相近的分片。 */
    List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK);

    /**
     * 带权重的混合检索。
     *
     * <p>权重决定两路召回对融合分的贡献。把某一路权重设为 0 即彻底关闭该路，
     * 可用 {@code (1, 0)} 表达「纯向量检索」，用于对照混合检索带来的效果差异。</p>
     *
     * <p>{@code minKeywordScore} 是关键词路的最低分。它和向量路的 {@code minScore}
     * 不同量纲（后者是余弦相似度、前者是 word_similarity），但都决定「这一路算不算命中」，
     * 因此必须能单独调——两路阈值不对称会直接影响「无答案时能否返回空结果」。</p>
     */
    List<ChunkMatch> search(
            String knowledgeBaseId,
            String userId,
            String query,
            int topK,
            double vectorWeight,
            double keywordWeight,
            double minKeywordScore);

    /**
     * 按知识库自身的阈值和 TopK 检索，只返回可以进入对话上下文的结果。
     */
    List<ChunkMatch> searchForConversation(String knowledgeBaseId, String userId, String query);
}
