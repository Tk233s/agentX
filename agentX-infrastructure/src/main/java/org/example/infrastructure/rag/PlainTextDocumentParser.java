package org.example.infrastructure.rag;

import org.example.domain.rag.adapter.port.DocumentParserPort;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

/**
 * 纯文本文档解析。
 *
 * 现在只认 txt / md / markdown：这三种本质都是文本，
 * PDF、Word 那类需要额外解析库的格式先不做，等真有需求再加一个实现类即可，
 * 领域服务只依赖 DocumentParserPort，不会因为换解析器而改动。
 */
@Component
public class PlainTextDocumentParser implements DocumentParserPort {

    private static final Set<String> SUPPORTED_TYPES = Set.of("txt", "md", "markdown");

    /** UTF-8 解不出来的字节会被替换成这个字符，用它可以反过来判断编码猜错了没有。 */
    private static final char REPLACEMENT_CHARACTER = '\uFFFD';

    /** 中文环境里最常见的"非 UTF-8"编码，Windows 记事本存出来的 txt 基本都是它。 */
    private static final Charset GBK = Charset.forName("GBK");

    @Override
    public String parse(String filePath, String fileType) {
        if (!supports(fileType)) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "暂不支持的文件类型：" + (fileType == null || fileType.isBlank() ? "未知" : fileType));
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "读取文件失败：" + rootMessage(e));
        }

        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.indexOf(REPLACEMENT_CHARACTER) >= 0) {
            // UTF-8 解码出现替换字符，说明原始字节不是 UTF-8，按 GBK 再解一次
            text = new String(bytes, GBK);
        }
        // 去掉 BOM，否则它会混进第一个分片里
        return text.replace("\uFEFF", "").trim();
    }

    @Override
    public boolean supports(String fileType) {
        return fileType != null && SUPPORTED_TYPES.contains(fileType.trim().toLowerCase(Locale.ROOT));
    }

    private String rootMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
