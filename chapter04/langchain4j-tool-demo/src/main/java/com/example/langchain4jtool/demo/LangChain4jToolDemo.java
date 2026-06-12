package com.example.langchain4jtool.demo;

import com.example.langchain4jtool.assistant.CommerceAssistant;
import com.example.langchain4jtool.assistant.CommerceAssistantFactory;
import com.example.langchain4jtool.service.CrmService;
import com.example.langchain4jtool.service.InventoryService;
import com.example.langchain4jtool.service.OrderService;
import com.example.langchain4jtool.service.ToolAuditLog;
import com.example.langchain4jtool.tool.CommerceTools;

public class LangChain4jToolDemo {

    public static void main(String[] args) {
        CommerceTools tools = new CommerceTools(
            new OrderService(),
            new InventoryService(),
            new CrmService(),
            new ToolAuditLog()
        );

        CommerceAssistant assistant = CommerceAssistantFactory.createWithOllama(tools);
        String answer = assistant.answer("帮我查一下订单 O-1001 的状态，并看看客户 C-2001 是什么等级。");
        System.out.println(answer);
    }
}
