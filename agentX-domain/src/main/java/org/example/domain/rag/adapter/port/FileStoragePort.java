package org.example.domain.rag.adapter.port;

/**
 * 上传文件存储端口。
 *
 * 现在落本地磁盘，将来换对象存储只需要换一个适配器。
 */
public interface FileStoragePort {

    /**
     * 保存文件并返回可再次读取的路径。
     *
     * @param originalFileName 原始文件名，用于保留扩展名
     * @param content          文件字节
     */
    String store(String originalFileName, byte[] content);

    /** 删除文件，不存在时静默返回。 */
    void delete(String filePath);
}
