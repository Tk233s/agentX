package org.example.infrastructure.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 文件读取工具 —— 列目录、读文件。
 *
 * 与写入工具拆成两个工具 ID，只读会话可以只挂本工具，
 * 因此"能读不能写"不再需要退化成"什么工具都不挂"。
 */
@AgentTool(
        value = "file-read",
        name = "文件读取",
        description = "在受限工作目录内列出目录内容或读取文本文件，不会修改任何文件。")
public class FileReadTool {

    private final WorkspacePathGuard guard;

    public FileReadTool() {
        // 默认使用 E:\Desktop（根据你实际的桌面路径）
        this("E:/Desktop");
    }

    public FileReadTool(String baseDir) {
        this.guard = new WorkspacePathGuard(baseDir);
    }

    /**
     * 列出工作目录下的所有文件。
     *
     * @return 文件列表（每行一个文件名）
     */
    @Tool(description = "列出工作目录下的所有文件和子目录")
    public String listFiles() {
        try {
            Path dir = guard.baseDirPath();
            if (!Files.exists(dir)) {
                return "目录不存在：" + guard.baseDir();
            }

            StringBuilder sb = new StringBuilder("目录内容（" + guard.baseDir() + "）：\n");
            try (Stream<Path> paths = Files.list(dir)) {
                paths.forEach(path -> {
                    String type = Files.isDirectory(path) ? "[目录]" : "[文件]";
                    sb.append(type).append(" ").append(path.getFileName()).append("\n");
                });
            }

            return sb.toString();
        } catch (IOException e) {
            return "列表失败：" + e.getMessage();
        }
    }

    /**
     * 读取文本文件的内容。
     *
     * @param fileName 文件名（不含路径）
     * @return 文件内容文本
     */
    @Tool(description = "读取指定文件的内容并返回文本")
    public String readFile(
            @ToolParam(description = "要读取的文件名") String fileName) {
        try {
            Path filePath = guard.resolve(fileName);

            if (!Files.exists(filePath)) {
                return "文件不存在：" + filePath;
            }

            return Files.readString(filePath);
        } catch (IOException e) {
            return "文件读取失败：" + e.getMessage();
        } catch (SecurityException e) {
            return "安全限制：" + e.getMessage();
        }
    }
}
