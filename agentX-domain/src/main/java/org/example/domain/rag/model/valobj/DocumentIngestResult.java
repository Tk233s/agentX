package org.example.domain.rag.model.valobj;

import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;

/**
 * 上传请求的结果。duplicate 为 true 时返回的是已存在的同内容文档。
 */
public record DocumentIngestResult(
        KnowledgeDocumentEntity document,
        boolean duplicate) {
}
