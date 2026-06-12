package com.example.codingagent;

import com.example.codingagent.workspace.WorkspaceBoundary;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceBoundaryTest {

    @Test
    void shouldRejectPathEscapingWorkspace() {
        WorkspaceBoundary boundary = new WorkspaceBoundary(Path.of("target/workspace-boundary").toAbsolutePath());

        assertThrows(SecurityException.class, () -> boundary.resolve("../secret.txt"));
    }

    @Test
    void shouldResolveRelativePathInsideWorkspace() {
        Path root = Path.of("target/workspace-boundary").toAbsolutePath().normalize();
        WorkspaceBoundary boundary = new WorkspaceBoundary(root);

        assertTrue(boundary.resolve("src/main/java/App.java").startsWith(root));
    }
}
