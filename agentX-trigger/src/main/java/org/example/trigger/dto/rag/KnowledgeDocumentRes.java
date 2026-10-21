package org.example.trigger.dto.rag;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档 HTTP 出参 DTO
 *
 * filePath 属于服务端内部信息，不出参。
 */
@Data
public class KnowledgeDocumentRes {

    private String id;

    private String knowledgeBaseId;

    /** 原始文件名 */
    private String fileName;

    /** 文件字节数 */
    private Long fileSize;

    private String fileType;

    /** 抽取出的字符数 */
    private Integer charCount;

    /** 分片数量 */
    private Integer chunkCount;

    /** PENDING / PROCESSING / READY / FAILED */
    private String status;

    /** 失败原因，成功时为 null */
    private String errorMessage;

    /** 后台处理尝试次数 */
    private Integer retryCount;

    /** 本次上传是否命中了同知识库内的相同内容 */
    private Boolean duplicate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
