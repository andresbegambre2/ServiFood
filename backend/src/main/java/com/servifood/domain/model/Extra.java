package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "extras")
public class Extra extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 120) @Column(nullable = false, unique = true, length = 120) private String name;
    @Size(max = 500) @Column(length = 500) private String description;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false) private boolean available = true;
    protected Extra() {}
    public Extra(String name, BigDecimal price) { this.name = name; this.price = price; }
    public Long getId() { return id; } public String getName() { return name; } public BigDecimal getPrice() { return price; }
    public boolean isAvailable() { return available; }
}

