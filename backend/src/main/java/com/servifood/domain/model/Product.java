package com.servifood.domain.model;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "products")
public class Product extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) private String name;
    @NotBlank @Size(max = 170) @Column(nullable = false, unique = true, length = 170) private String slug;
    @NotBlank @Size(max = 2000) @Column(nullable = false, length = 2000) private String description;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Size(max = 500) @Column(name = "image_path", length = 500) private String imagePath;
    @Column(nullable = false) private boolean available = true;
    @Column(nullable = false) private boolean featured;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id", nullable = false) private Category category;
    @ManyToMany
    @JoinTable(name = "product_extras", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "extra_id"))
    private Set<Extra> allowedExtras = new LinkedHashSet<>();
    protected Product() {}
    public Product(String name, String slug, String description, BigDecimal price, Category category) {
        this.name = name; this.slug = slug; this.description = description; this.price = price; this.category = category;
    }
    public void allowExtra(Extra extra) { allowedExtras.add(extra); }
    public void changePrice(BigDecimal newPrice) { this.price = newPrice; }
    public Long getId() { return id; } public String getName() { return name; } public String getSlug() { return slug; }
    public BigDecimal getPrice() { return price; } public Category getCategory() { return category; }
    public Set<Extra> getAllowedExtras() { return Set.copyOf(allowedExtras); }
    public boolean isAvailable() { return available; } public boolean isFeatured() { return featured; }
}
