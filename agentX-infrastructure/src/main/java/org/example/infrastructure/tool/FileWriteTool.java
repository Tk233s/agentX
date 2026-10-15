package org.example.infrastructure.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具 —— 创建/覆盖、追加。
 *
 * 与读取工具拆成两个工具 ID，只读会话不挂本工具，
 * 从工具清单层面就切断了写入能力。
 */
@AgentTool(
        value = "file-write",
        name = "文件写入",
        description = "在受限工作目录内创建、覆盖或追加文本文件。",
        mutating = true)
public class FileWriteTool {

    private final WorkspacePathGuard guard;

    public FileWriteTool() {
        // 默认使用 E:\Desktop（根据你实际的桌面路径）
        this("E:/Desktop");
    }

    public FileWriteTool(String baseDir) {
        this.guard = new WorkspacePathGuard(baseDir);
    }

    /**
     * 创建或覆盖一个文本文件。
     *
     * @param fileName 文件名（不含路径），如 "weather.txt"、"report.md"
     * @param content  要写入的文本内容
     * @return 操作结果描述
     */
    @Tool(description = "创建一个文本文件并写入内容。如果文件已存在则覆盖。")
    public String writeFile(
            @ToolParam(description = "文件名，如 weather.txt、report.md") String fileName,
            @ToolParam(description = "要写入文件的文本内容") String content) {
        try {
            // 路径校验：防止 LLM 传入 ../etc/passwd 之类的恶意路径
            Path filePath = guard.resolve(fileName);

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
     * 追加内容到文件末尾。
     *
     * @param fileName 文件名
     * @param content  要追加的内容
     * @return 操作结果描述
     */
    @Tool(description = "在已有文件的末尾追加内容")
    public String appendFile(
            @ToolParam(description = "文件名") String fileName,
            @ToolParam(description = "要追加到文件末尾的文本内容") String content) {
        try {
            Path filePath = guard.resolve(fileName);

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
}
