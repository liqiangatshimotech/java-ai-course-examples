package com.example.chapter09.sourcemap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ClaudeCodeSourceCatalogDemo {

    // 这些文件是 9.1 文档里反复出现的关键模块。demo 不依赖 TypeScript 运行时，
    // 只扫描 source map 还原出的源码目录，用来验证“架构精读”引用的文件确实存在。
    private static final List<Spotlight> SPOTLIGHTS = List.of(
            new Spotlight("main.tsx", "顶层入口，先做 profiling、MDM、Keychain 和 feature flag 预热。"),
            new Spotlight("commands.ts", "统一命令注册入口，决定哪些 /commands /skills /mcp 可见。"),
            new Spotlight("skills/loadSkillsDir.ts", "读取 skill frontmatter、组装 prompt、执行内嵌 shell。"),
            new Spotlight("utils/skills/skillChangeDetector.ts", "watch + debounce + cache clear，把技能热更新做成守护进程。"),
            new Spotlight("utils/toolPool.ts", "合并 built-in / MCP 工具并保持稳定排序。"),
            new Spotlight("utils/permissions/permissions.ts", "allow / ask / deny 的权限引擎。"),
            new Spotlight("services/mcp/client.ts", "MCP transport、auth、tool bridge。"),
            new Spotlight("bridge/bridgeMain.ts", "Remote / Desktop / Web bridge 的本地控制层。"),
            new Spotlight("utils/plugins/pluginLoader.ts", "插件发现、验证和加载。"),
            new Spotlight("coordinator/coordinatorMode.ts", "多 Agent 协调模式的过滤层。"),
            new Spotlight("assistant/sessionHistory.ts", "Assistant 会话历史的加载与整理。"),
            new Spotlight("bootstrap/state.ts", "session、cwd、mode、prompt context 等运行态。"));

    public void run() {
        Path sourceRoot = locateSourceRoot();
        SourceCatalog catalog = scan(sourceRoot);

        System.out.println("[1] Claude Code source map");
        System.out.println("Source root: " + sourceRoot);
        System.out.println("Indexed files: " + catalog.fileCount());
        System.out.println("Top-level buckets:");
        catalog.bucketCounts().forEach((bucket, count) ->
                System.out.println("  - " + bucket + ": " + count));

        System.out.println("Spotlight modules:");
        for (Spotlight spotlight : SPOTLIGHTS) {
            Path file = sourceRoot.resolve(spotlight.relativePath());
            String mark = Files.exists(file) ? "ok" : "missing";
            System.out.println("  - " + spotlight.relativePath() + " [" + mark + "] " + spotlight.note());
        }
    }

    private SourceCatalog scan(Path sourceRoot) {
        Map<String, Long> rawCounts;
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            // 这里只做轻量索引：统计每个顶层目录下有多少源码文件。
            // 真实项目可以在这里继续扩展 AST 解析、依赖图构建或关键 symbol 索引。
            rawCounts = walk
                    .filter(Files::isRegularFile)
                    .filter(this::isSourceFile)
                    .map(sourceRoot::relativize)
                    .collect(Collectors.groupingBy(
                            this::classifyBucket,
                            Collectors.counting()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan Claude Code source root: " + sourceRoot, e);
        }

        Map<String, Long> ordered = rawCounts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        Map::putAll);

        long fileCount = rawCounts.values().stream().mapToLong(Long::longValue).sum();
        return new SourceCatalog(sourceRoot, fileCount, ordered);
    }

    private Path locateSourceRoot() {
        String configured = System.getenv("CLAUDE_CODE_SOURCEMAP_ROOT");
        if (configured != null && !configured.isBlank()) {
            // 支持两种传参方式：传仓库根目录，或者直接传 restored-src/src。
            Path candidate = Path.of(configured).toAbsolutePath().normalize();
            Path restoredSrc = candidate.resolve("restored-src/src");
            if (Files.isDirectory(restoredSrc)) {
                return restoredSrc;
            }
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }

        Path cursor = Path.of("").toAbsolutePath().normalize();
        while (cursor != null) {
            // 从当前目录向上查找，方便在 java-ai-course-examples 或子模块目录里直接运行 Maven。
            Path candidate = cursor.resolve("claude-code-sourcemap/restored-src/src");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }

        throw new IllegalStateException("Cannot locate claude-code-sourcemap/restored-src/src from current working directory.");
    }

    private boolean isSourceFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".ts") || name.endsWith(".tsx") || name.endsWith(".js") || name.endsWith(".mjs");
    }

    private String classifyBucket(Path relativePath) {
        // 根目录文件没有天然模块名，这里把 9.1 里关注的文件映射成人能读懂的架构分组。
        if (relativePath.getNameCount() == 1) {
            String file = relativePath.getFileName().toString();
            return switch (file) {
                case "main.tsx" -> "boot";
                case "commands.ts" -> "command-registry";
                case "tools.js", "tools.ts" -> "tool-registry";
                case "Task.ts", "Tool.ts", "QueryEngine.ts" -> "core-runtime";
                default -> "root";
            };
        }

        String first = relativePath.getName(0).toString();
        return switch (first) {
            case "commands", "tools", "services", "utils", "bridge", "skills",
                    "plugins", "coordinator", "assistant", "bootstrap",
                    "context", "state", "components", "cli", "voice", "vim" -> first;
            default -> first;
        };
    }

    private record SourceCatalog(Path root, long fileCount, Map<String, Long> bucketCounts) {
    }

    private record Spotlight(String relativePath, String note) {
        Spotlight {
            Objects.requireNonNull(relativePath, "relativePath");
            Objects.requireNonNull(note, "note");
        }
    }
}
