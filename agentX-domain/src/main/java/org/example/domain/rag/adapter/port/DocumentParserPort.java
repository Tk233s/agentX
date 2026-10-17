package org.example.domain.rag.adapter.port;

/**
 * 文档文本抽取端口。
 *
 * 领域层只关心"拿到纯文本"，文件怎么读、编码怎么猜、以后要不要支持 PDF，都是适配器的事。
 */
public interface DocumentParserPort {

    /**
     * 把文件解析成纯文本。
     *
     * @param filePath 落盘路径
     * @param fileType 扩展名，如 txt、md
     */
    String parse(String filePath, String fileType);

    /** 当前是否支持该扩展名。 */
    boolean supports(String fileType);
}
