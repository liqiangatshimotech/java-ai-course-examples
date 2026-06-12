package com.example.springaimcpclient.service;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class SupportCopilot {

    private final ChatClientRegistry registry;

    private final ToolCallbackProvider mcpToolCallbacks;

    public SupportCopilot(ChatClientRegistry registry, ObjectProvider<ToolCallbackProvider> mcpToolCallbacksProvider) {
        this.registry = registry;
        this.mcpToolCallbacks = mcpToolCallbacksProvider.getIfAvailable(ToolCallbackProvider::from);
    }

    public String answer(String provider, String question) {
        return registry.clientFor(provider)
            .prompt()
            .system("""
                你是售后支持助手。用户提到订单、退款、物流或政策时，必须先调用可用的 MCP 工具。
                如果工具没有返回足够信息，要明确说明还缺少什么，不要补写不存在的业务事实。
                """)
            .user(question)
            .toolCallbacks(mcpToolCallbacks)
            .call()
            .content();
    }
}
