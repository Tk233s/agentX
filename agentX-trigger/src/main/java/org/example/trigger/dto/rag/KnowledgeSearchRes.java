package org.example.trigger.dto.rag;

import lombok.Data;

/**
 * 知识库检索命中的片段出参 DTO
 *
 * score 直接给前端，方便调参时肉眼判断哪些片段是真的相关。
 */
@Data
public class KnowledgeSearchRes {

    private String chunkId;

    private String documentId;

    /** 来源文档名，用于展示出处 */
    private String documentName;

    private String content;

    private Double score;
}
