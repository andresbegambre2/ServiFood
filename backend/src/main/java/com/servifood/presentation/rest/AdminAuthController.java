package com.servifood.presentation.rest;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.servifood.infrastructure.persistence.InternalUserRepository;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final InternalUserRepository users;

    public AdminAuthController(InternalUserRepository users) { this.users = users; }

    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }

    @GetMapping("/session")
    AdminSessionResponse session(Authentication authentication) {
        var user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return new AdminSessionResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    record AdminSessionResponse(Long id, String name, String email, String role) {}
}
