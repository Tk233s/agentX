package org.example.infrastructure.tool;

import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件操作 Tool —— Function Calling 演示
 *
 * LLM 本身没有读写文件的能力，通过这个 Tool，AI 可以：
 * - 创建新文件
 * - 读取已有文件内容
 * - 追加内容到文件
 * - 列出目录下的文件
 *
 * 安全考虑：
 * - 限制了只能操作指定目录（如工作桌面、工作目录），防止 LLM 乱删系统文件
 * - 路径校验防止目录遍历攻击（../ 跳到上级目录）
 */
@Component
public class FileTool {

    /**
     * 允许操作的基础目录。
     * 实际项目中可以配置为用户的专属工作空间目录。
     */
    private final String baseDir;

    public FileTool() {
        // 默认使用 E:\Desktop（根据你实际的桌面路径）
        this("E:/Desktop");
    }

    public FileTool(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * 创建或覆盖一个文本文件。
     *
     * @param fileName 文件名（不含路径），如 "weather.txt"、"report.md"
     * @param content  要写入的文本内容
     * @return 操作结果描述
     */
    @org.springframework.ai.tool.annotation.Tool(description = "创建一个文本文件并写入内容。如果文件已存在则覆盖。")
    public String writeFile(
            @org.springframework.ai.tool.annotation.ToolParam(description = "文件名，如 weather.txt、report.md") String fileName,
            @org.springframework.ai.tool.annotation.ToolParam(description = "要写入文件的文本内容") String content) {
        try {
            // 路径校验：防止 LLM 传入 ../etc/passwd 之类的恶意路径
            Path filePath = validateAndResolve(fileName);

            // 确保父目录存在
            Files.createDirectories(filePath.getParent());

            // 写入文件（覆盖模式）
            Files.writeString(filePath, content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            return "文件创建成功：" + filePath + "（" + content.length() + " 个字符）";
        } catch (IOException e) {
            return "文件写入失败：" + e.getMessage();
        } catch (SecurityException e) {
            return "安全限制：" + e.getMessage();
        }
    }

    /**
     * 读取文本文件的内容。
     *
     * @param fileName 文件名（不含路径）
     * @return 文件内容文本
     */
    @org.springframework.ai.tool.annotation.Tool(description = "读取指定文件的内容并返回文本")
    public String readFile(
            @org.springframework.ai.tool.annotation.ToolParam(description = "要读取的文件名") String fileName) {
        try {
            Path filePath = validateAndResolve(fileName);

            if (!Files.exists(filePath)) {
                return "文件不存在：" + filePath;
            }

            String content = Files.readString(filePath);
            return content;
        } catch (IOException e) {
            return "文件读取失败：" + e.getMessage();
        } catch (SecurityException e) {
            return "安全限制：" + e.getMessage();
        }
    }

    /**
     * 追加内容到文件末尾。
     *
     * @param fileName 文件名
     * @param content  要追加的内容
     * @return 操作结果描述
     */
    @org.springframework.ai.tool.annotation.Tool(description = "在已有文件的末尾追加内容")
    public String appendFile(
            @org.springframework.ai.tool.annotation.ToolParam(description = "文件名") String fileName,
            @org.springframework.ai.tool.annotation.ToolParam(description = "要追加到文件末尾的文本内容") String content) {
        try {
            Path filePath = validateAndResolve(fileName);

            // 如果文件不存在则创建
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
            }

            // 追加内容（带换行）
            Files.writeString(filePath,
                    System.lineSeparator() + content,
                    StandardOpenOption.APPEND);

            return "内容已追加到文件：" + filePath;
        } catch (IOException e) {
            return "追加失败：" + e.getMessage();
        } catch (SecurityException e) {
            return "安全限制：" + e.getMessage();
        }
    }

    /**
     * 列出基础目录下的所有文件。
     *
     * @return 文件列表（每行一个文件名）
     */
    @org.springframework.ai.tool.annotation.Tool(description = "列出工作目录下的所有文件和子目录")
    public String listFiles() {
        try {
            Path dir = Paths.get(baseDir);
            if (!Files.exists(dir)) {
                return "目录不存在：" + baseDir;
            }

            StringBuilder sb = new StringBuilder("目录内容（" + baseDir + "）：\n");
            Files.list(dir).forEach(path -> {
                String type = Files.isDirectory(path) ? "[目录]" : "[文件]";
                sb.append(type).append(" ").append(path.getFileName()).append("\n");
            });

            return sb.toString();
        } catch (IOException e) {
            return "列表失败：" + e.getMessage();
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 校验文件名，防止目录遍历攻击（Path Traversal）。
     *
     * LLM 可能传入 "../../etc/passwd" 或绝对路径 "/etc/passwd"，
     * 这个方法确保最终路径一定在 baseDir 范围内。
     */
    private Path validateAndResolve(String fileName) {
        // 拒绝包含路径分隔符的文件名（防止 LLM 传入路径）
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new SecurityException("文件名不能包含路径分隔符：" + fileName);
        }

        Path resolved = Paths.get(baseDir, fileName).normalize();

        // 解析后再次检查是否在 baseDir 内
        if (!resolved.startsWith(baseDir)) {
            throw new SecurityException("不允许访问工作目录之外的文件");
        }

        return resolved;
    }
}
