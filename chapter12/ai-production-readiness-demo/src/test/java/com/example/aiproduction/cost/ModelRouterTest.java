package com.example.aiproduction.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModelRouterTest {

    @Test
    void routesSimpleTaskToSmallModel() {
        var decision = new ModelRouter().route("普通摘要", new TokenUsage(200, 300), false);

        assertEquals("deepseek-chat", decision.selectedModel().modelName());
    }

    @Test
    void routesComplexTaskToStrongModel() {
        var decision = new ModelRouter().route("代码推理", new TokenUsage(10_000, 2_000), false);

        assertEquals("deepseek-reasoner", decision.selectedModel().modelName());
        assertTrue(decision.reason().contains("强模型"));
    }
}
