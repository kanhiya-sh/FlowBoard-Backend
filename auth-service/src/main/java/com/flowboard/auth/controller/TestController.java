package com.flowboard.auth.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/user")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    public String userApi() {
        return "User API working!";
    }

    @GetMapping("/api/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminApi() {
        return "Admin API working!";
    }

    @GetMapping("/api/public")
    public String publicApi() {
        return "Public API working!";
    }

    @GetMapping("/api/me")
    public Object me(Authentication auth) {
        return auth.getPrincipal();
    }
}