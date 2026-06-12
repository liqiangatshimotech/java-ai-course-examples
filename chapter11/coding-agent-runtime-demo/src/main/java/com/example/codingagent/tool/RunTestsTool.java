package com.example.codingagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * 演示用的测试工具。
 * 真实系统可以把这里替换成 mvn test、gradle test 或企业内部 CI API。
 */
public final class RunTestsTool implements AgentTool {

    @Override
    public String name() {
        return "run_tests";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.MEDIUM;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException {
        String source = Files.readString(context.boundary()
                .resolve("src/main/java/com/acme/billing/PriceCalculator.java"));
        String testSource = Files.readString(context.boundary()
                .resolve("src/test/java/com/acme/billing/PriceCalculatorTest.java"));

        boolean discountLimitChanged = source.contains("new BigDecimal(\"0.30\")");
        boolean methodSignatureKept = source.contains("calculateFinalPrice(BigDecimal originalPrice, BigDecimal discountRate)");
        boolean testIntentPresent = testSource.contains("thirtyPercentDiscount");

        if (discountLimitChanged && methodSignatureKept && testIntentPresent) {
            return ToolResult.success("测试通过",
                    "[PASS] accepts thirtyPercentDiscount%n[PASS] rejects discount above max%n"
                            .formatted());
        }
        return ToolResult.failure("测试失败",
                "需要同时满足：折扣上限为 0.30、方法签名不变、测试文件包含 thirtyPercentDiscount。");
    }
}
