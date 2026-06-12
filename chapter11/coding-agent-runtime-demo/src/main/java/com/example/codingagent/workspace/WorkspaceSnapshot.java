package com.example.codingagent.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * 记录运行前的文本文件快照，用于最后生成 diff。
 */
public record WorkspaceSnapshot(Map<String, String> files) {

    public static WorkspaceSnapshot capture(Path root) throws IOException {
        Map<String, String> files = new TreeMap<>();
        try (var stream = Files.walk(root)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                if (isTextFile(path)) {
                    files.put(root.relativize(path).toString(), Files.readString(path));
                }
            }
        }
        return new WorkspaceSnapshot(Map.copyOf(files));
    }

    private static boolean isTextFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".md") || name.endsWith(".xml")
                || name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties");
    }
}
