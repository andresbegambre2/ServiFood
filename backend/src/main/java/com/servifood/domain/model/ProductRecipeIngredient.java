package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "product_recipe_ingredients", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "ingredient_id"}))
public class ProductRecipeIngredient extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ingredient_id", nullable = false) private Ingredient ingredient;
    @NotNull @DecimalMin("0.001") @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity;
    protected ProductRecipeIngredient() {}
    public ProductRecipeIngredient(Product product, Ingredient ingredient, BigDecimal quantity) { this.product = product; this.ingredient = ingredient; this.quantity = quantity.setScale(3); }
    public Long getId() { return id; } public Product getProduct() { return product; } public Ingredient getIngredient() { return ingredient; } public BigDecimal getQuantity() { return quantity; }
}
