package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "extra_recipe_ingredients", uniqueConstraints = @UniqueConstraint(columnNames = {"extra_id", "ingredient_id"}))
public class ExtraRecipeIngredient extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "extra_id", nullable = false) private Extra extra;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ingredient_id", nullable = false) private Ingredient ingredient;
    @NotNull @DecimalMin("0.001") @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity;
    protected ExtraRecipeIngredient() {}
    public ExtraRecipeIngredient(Extra extra, Ingredient ingredient, BigDecimal quantity) { this.extra = extra; this.ingredient = ingredient; this.quantity = quantity.setScale(3); }
    public Long getId() { return id; } public Extra getExtra() { return extra; } public Ingredient getIngredient() { return ingredient; } public BigDecimal getQuantity() { return quantity; }
}
