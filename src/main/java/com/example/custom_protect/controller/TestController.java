package com.example.custom_protect.controller;

import com.example.custom_protect.aop.JwtAuth;
import com.example.custom_protect.aop.JwtAuthWithRoles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is public";
    }

    @GetMapping("/secure")
    @JwtAuth
    public String secureEndpoint() {
        return "This is secured by JWT only";
    }

    @GetMapping("/admin")
    @JwtAuthWithRoles(allowedRoles = {"ROLE_ADMIN"})
    public String adminEndpoint() {
        return "This is admin only";
    }

    @GetMapping("/authenticated")
    @JwtAuthWithRoles(allowedRoles = {"ROLE_USER", "ROLE_ADMIN"})
    public String userOrAdminEndpoint() {
        return "This is available to USER or ADMIN";
    }
}
