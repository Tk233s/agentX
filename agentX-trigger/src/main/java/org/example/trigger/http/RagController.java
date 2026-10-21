package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.rag.adapter.repository.KnowledgeChunkRepository;
import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.model.valobj.DocumentIngestResult;
import org.example.domain.rag.service.IRagDomainService;
import org.example.trigger.dto.rag.KnowledgeBaseReq;
import org.example.trigger.dto.rag.KnowledgeBaseRes;
import org.example.trigger.dto.rag.KnowledgeDocumentRes;
import org.example.trigger.dto.rag.KnowledgeSearchReq;
import org.example.trigger.dto.rag.KnowledgeSearchRes;
import org.example.trigger.dto.rag.KnowledgeRetrievalConfigReq;
import org.example.types.context.UserContext;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 知识库管理。
 *
 * 覆盖"建库 → 传文档 → 看状态 → 检索测试"这一条链路，
 * 检索测试接口的作用是让绑 Agent 之前就能确认库里真的能搜到东西。
 */
@RestController
@RequestMapping("/rag/knowledge-base")
public class RagController {

    @Resource
    private IRagDomainService ragDomainService;

    @Resource
    private KnowledgeChunkRepository knowledgeChunkRepository;

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public Response<KnowledgeBaseRes> create(@RequestBody @Validated KnowledgeBaseReq req) {
        KnowledgeBaseEntity entity = RagAssembler.toEntity(req, UserContext.requireCurrentUserId());
        KnowledgeBaseEntity created = ragDomainService.createKnowledgeBase(entity);
        return Response.success(RagAssembler.toRes(created, 0));
    }

    /**
     * 查询当前用户的知识库列表
     */
    @GetMapping("/list")
    public Response<List<KnowledgeBaseRes>> list() {
        String userId = UserContext.requireCurrentUserId();
        List<KnowledgeBaseRes> resList = ragDomainService.listKnowledgeBases(userId).stream()
                .map(entity -> RagAssembler.toRes(
                        entity,
                        knowledgeChunkRepository.countByKnowledgeBaseId(entity.getId())))
                .toList();
        return Response.success(resList);
    }

    /**
     * 根据ID查询知识库
     */
    @GetMapping("/get")
    public Response<KnowledgeBaseRes> get(@RequestParam("id") String id) {
        KnowledgeBaseEntity entity = ragDomainService.getKnowledgeBase(id, UserContext.requireCurrentUserId());
        return Response.success(RagAssembler.toRes(
                entity,
                knowledgeChunkRepository.countByKnowledgeBaseId(entity.getId())));
    }

    /**
     * 更新对话检索阈值和 TopK；只影响后续对话，不需要重新向量化。
     */
    @PostMapping("/retrieval-config")
    public Response<KnowledgeBaseRes> updateRetrievalConfig(
            @RequestBody @Validated KnowledgeRetrievalConfigReq req) {
        String userId = UserContext.requireCurrentUserId();
        KnowledgeBaseEntity updated = ragDomainService.updateRetrievalConfig(
                req.getId(),
                userId,
                req.getMinScore(),
                req.getTopK());
        return Response.success(RagAssembler.toRes(
                updated,
                knowledgeChunkRepository.countByKnowledgeBaseId(updated.getId())));
    }

    /**
     * 删除知识库（连带文档、分片与落盘文件）
     */
    @PostMapping("/delete")
    public Response<Void> delete(@RequestParam("id") String id) {
        ragDomainService.deleteKnowledgeBase(id, UserContext.requireCurrentUserId());
        return Response.success();
    }

    /**
     * 上传文档并立即返回 PENDING 记录，解析、分片和向量化由后台线程继续处理。
     */
    @PostMapping("/document/upload")
    public Response<KnowledgeDocumentRes> upload(
            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
            @RequestParam("file") MultipartFile file) {
        String userId = UserContext.requireCurrentUserId();
        if (file == null || file.isEmpty()) {
            return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "请选择要上传的文件");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "读取上传文件失败：" + e.getMessage());
        }
        DocumentIngestResult result = ragDomainService.ingestDocument(
                knowledgeBaseId,
                userId,
                file.getOriginalFilename(),
                content);
        return Response.success(RagAssembler.toRes(result.document(), result.duplicate()));
    }

    /**
     * 查询单份文档状态，供前端上传后轮询。
     */
    @GetMapping("/document/get")
    public Response<KnowledgeDocumentRes> getDocument(@RequestParam("id") String id) {
        return Response.success(RagAssembler.toRes(
                ragDomainService.getDocument(id, UserContext.requireCurrentUserId())));
    }

    /**
     * 重试处理失败或中断的文档。
     */
    @PostMapping("/document/retry")
    public Response<KnowledgeDocumentRes> retryDocument(@RequestParam("id") String id) {
        return Response.success(RagAssembler.toRes(
                ragDomainService.retryDocument(id, UserContext.requireCurrentUserId())));
    }

    /**
     * 查询知识库下的文档列表
     */
    @GetMapping("/document/list")
    public Response<List<KnowledgeDocumentRes>> listDocuments(
            @RequestParam("knowledgeBaseId") String knowledgeBaseId) {
        List<KnowledgeDocumentRes> resList = ragDomainService
                .listDocuments(knowledgeBaseId, UserContext.requireCurrentUserId())
                .stream()
                .map(RagAssembler::toRes)
                .toList();
        return Response.success(resList);
    }

    /**
     * 删除文档（连带其分片与落盘文件）
     */
    @PostMapping("/document/delete")
    public Response<Void> deleteDocument(@RequestParam("id") String id) {
        ragDomainService.deleteDocument(id, UserContext.requireCurrentUserId());
        return Response.success();
    }

    /**
     * 检索测试：用一句话去库里找最相近的分片，不经过模型。
     */
    @PostMapping("/search")
    public Response<List<KnowledgeSearchRes>> search(@RequestBody @Validated KnowledgeSearchReq req) {
        List<ChunkMatch> matches = ragDomainService.search(
                req.getKnowledgeBaseId(),
                UserContext.requireCurrentUserId(),
                req.getQuery(),
                req.getTopK() == null ? IRagDomainService.DEFAULT_TOP_K : req.getTopK());
        return Response.success(matches.stream().map(RagAssembler::toRes).toList());
    }
}
