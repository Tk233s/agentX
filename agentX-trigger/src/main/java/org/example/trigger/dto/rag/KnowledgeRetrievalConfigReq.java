package org.example.trigger.dto.rag;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 更新知识库的对话检索参数。
 */
@Data
public class KnowledgeRetrievalConfigReq {

    @NotBlank(message = "知识库ID不能为空")
    private String id;

    @DecimalMin(value = "0.0", message = "最低相似度不能小于0")
    @DecimalMax(value = "1.0", message = "最低相似度不能大于1")
    private Double minScore;

    @Min(value = 1, message = "TopK不能小于1")
    @Max(value = 20, message = "TopK不能大于20")
    private Integer topK;
}
