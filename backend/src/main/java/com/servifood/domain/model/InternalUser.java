package com.servifood.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "internal_users")
public class InternalUser extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120)
    private String name;
    @NotBlank @Email @Size(max = 190) @Column(nullable = false, unique = true, length = 190)
    private String email;
    @NotBlank @Size(max = 255) @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private UserRole role;
    @Column(nullable = false)
    private boolean active = true;

    protected InternalUser() {}
    public InternalUser(String name, String email, String passwordHash, UserRole role) {
        this.name = name; this.email = email.toLowerCase(); this.passwordHash = passwordHash; this.role = role;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
}

