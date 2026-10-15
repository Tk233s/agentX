package org.example.infrastructure.tool;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工作目录内的路径校验，供文件读取与写入工具共用。
 *
 * LLM 可能传入 "../../etc/passwd" 或绝对路径，这里确保最终路径一定落在工作目录内。
 * 校验规则只维护一份，避免读写两个工具出现安全口径不一致。
 */
final class WorkspacePathGuard {

    /**
     * 允许操作的基础目录。
     * 实际项目中可以配置为用户的专属工作空间目录。
     */
    private final String baseDir;

    WorkspacePathGuard(String baseDir) {
        this.baseDir = baseDir;
    }

    String baseDir() {
        return baseDir;
    }

    Path baseDirPath() {
        return Paths.get(baseDir);
    }

    /**
     * 校验文件名并解析成工作目录内的路径。
     *
     * @param fileName 文件名（不含路径）
     * @return 落在工作目录内的解析结果
     */
    Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new SecurityException("文件名不能为空");
        }
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
