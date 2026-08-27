package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ingredient_id", nullable = false) private Ingredient ingredient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private InternalUser createdBy;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 20) private InventoryMovementType type;
    @NotNull @Column(name = "quantity_delta", nullable = false, precision = 14, scale = 3) private BigDecimal quantityDelta;
    @NotNull @DecimalMin("0.000") @Column(name = "balance_after", nullable = false, precision = 14, scale = 3) private BigDecimal balanceAfter;
    @NotBlank @Size(max = 500) @Column(nullable = false, length = 500) private String reason;
    protected InventoryMovement() {}
    public InventoryMovement(Ingredient ingredient, CustomerOrder order, InternalUser createdBy, InventoryMovementType type, BigDecimal quantityDelta, String reason) { this.ingredient = ingredient; this.order = order; this.createdBy = createdBy; this.type = type; this.quantityDelta = quantityDelta.setScale(3); this.balanceAfter = ingredient.getStockCurrent(); this.reason = reason.trim(); }
    public Long getId() { return id; } public Ingredient getIngredient() { return ingredient; } public CustomerOrder getOrder() { return order; }
    public InternalUser getCreatedBy() { return createdBy; } public InventoryMovementType getType() { return type; }
    public BigDecimal getQuantityDelta() { return quantityDelta; } public BigDecimal getBalanceAfter() { return balanceAfter; } public String getReason() { return reason; }
}
