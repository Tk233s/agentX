package org.example.infrastructure.rag;

import org.example.domain.rag.adapter.port.FileStoragePort;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 上传文件的本机磁盘存储。
 *
 * 落盘规则：{uploadDir}/yyyyMM/{随机ID}.{扩展名}
 * 按月份分目录是为了避免单个目录堆几十万个文件；
 * 文件名用随机 ID 而不是原文件名，是为了避免同名覆盖和用户文件名里的路径字符带来的问题。
 *
 * 换成 OSS、MinIO 时只需要再写一个 FileStoragePort 实现。
 */
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageAdapter.class);

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /** 扩展名最长保留的字符数，够用且能挡住异常长的文件名尾部。 */
    private static final int MAX_EXTENSION_LENGTH = 16;

    /** 上传根目录，可通过配置覆盖，默认落在项目工作目录下的 data/rag。 */
    @Value("${agentx.rag.upload-dir:./data/rag}")
    private String uploadDir;

    @Override
    public String store(String originalFileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "上传的文件内容为空");
        }
        Path directory = Paths.get(uploadDir, LocalDate.now().format(MONTH_FORMATTER));
        Path target = directory.resolve(
                UUID.randomUUID().toString().replace("-", "") + extensionOf(originalFileName));
        try {
            Files.createDirectories(directory);
            Files.write(target, content);
        } catch (IOException e) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "文件落盘失败：" + rootMessage(e));
        }
        return target.toAbsolutePath().normalize().toString();
    }

    @Override
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // 文件删不掉不该让业务失败：数据库里的记录已经清干净了，留个日志即可
            log.warn("删除上传文件失败，path={}: {}", filePath, e.getMessage());
        }
    }

    /**
     * 取出扩展名（含点号）。
     *
     * 只保留字母数字，".."、"/" 这类字符会被丢掉，
     * 拼接出来的路径因此不可能跳出 uploadDir。
     */
    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String raw = fileName.substring(dot + 1);
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < raw.length() && cleaned.length() < MAX_EXTENSION_LENGTH; i++) {
            char current = raw.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                cleaned.append(Character.toLowerCase(current));
            }
        }
        return cleaned.isEmpty() ? "" : "." + cleaned.toString().toLowerCase(Locale.ROOT);
    }

    private String rootMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
