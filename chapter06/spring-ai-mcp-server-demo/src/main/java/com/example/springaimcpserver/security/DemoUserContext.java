package com.example.springaimcpserver.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DemoUserContext {

    public CurrentUser currentUser() {
        // 生产环境不能写死用户。这里应从 OAuth token、session 或网关透传的身份中解析。
        return new CurrentUser("u-1001", "acme", Set.of("support_agent"));
    }
}
