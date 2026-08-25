package com.servifood.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 100) @Column(nullable = false, unique = true, length = 100) private String name;
    @NotBlank @Size(max = 120) @Column(nullable = false, unique = true, length = 120) private String slug;
    @Size(max = 500) @Column(length = 500) private String description;
    @PositiveOrZero @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private boolean active = true;
    protected Category() {}
    public Category(String name, String slug, int displayOrder) { this.name = name; this.slug = slug; this.displayOrder = displayOrder; }
    public Long getId() { return id; } public String getName() { return name; } public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public int getDisplayOrder() { return displayOrder; } public boolean isActive() { return active; }
    public void deactivate() { active = false; }
}
