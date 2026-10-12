package com.example.llmdatadiscovery.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // 1. Authentication Service
    @GetMapping
    public Map<String, String> authenticate(Authentication authentication) {
        return Map.of(
                "message", "Authenticated successfully",
                "user", authentication.getName()
        );
    }
}