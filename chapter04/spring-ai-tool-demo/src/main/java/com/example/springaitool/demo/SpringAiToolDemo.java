package com.example.springaitool.demo;

import com.example.springaitool.service.CrmService;
import com.example.springaitool.service.InventoryService;
import com.example.springaitool.service.OrderService;
import com.example.springaitool.service.ToolAuditLog;
import com.example.springaitool.tool.CommerceTools;

public class SpringAiToolDemo {

    public static void main(String[] args) {
        CommerceTools tools = new CommerceTools(
            new OrderService(),
            new InventoryService(),
            new CrmService(),
            new ToolAuditLog()
        );

        System.out.println(tools.queryOrderStatus("O-1001"));
        System.out.println(tools.queryInventory("SKU-KEYBOARD"));
        System.out.println(tools.queryCustomerProfile("C-2001"));
    }
}
