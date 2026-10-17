package org.example.trigger.dto.rag;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库检索测试入参 DTO
 */
@Data
public class KnowledgeSearchReq {

    @NotBlank(message = "知识库ID不能为空")
    private String knowledgeBaseId;

    /** 查询语句，一般是用户的问题 */
    @NotBlank(message = "查询内容不能为空")
    private String query;

    /** 返回条数；留空按领域服务默认值处理 */
    private Integer topK;
}
