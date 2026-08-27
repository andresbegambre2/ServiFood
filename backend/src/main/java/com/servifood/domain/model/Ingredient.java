package com.servifood.domain.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.servifood.domain.exception.DomainException;

@Entity
@Table(name = "ingredients")
public class Ingredient extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 150) @Column(nullable = false, unique = true, length = 150) private String name;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private IngredientUnit unit;
    @NotNull @DecimalMin("0.000") @Digits(integer = 11, fraction = 3) @Column(name = "stock_current", nullable = false, precision = 14, scale = 3) private BigDecimal stockCurrent;
    @NotNull @DecimalMin("0.000") @Digits(integer = 11, fraction = 3) @Column(name = "stock_minimum", nullable = false, precision = 14, scale = 3) private BigDecimal stockMinimum;
    @DecimalMin("0.0000") @Digits(integer = 10, fraction = 4) @Column(name = "unit_cost", precision = 14, scale = 4) private BigDecimal unitCost;
    @Column(nullable = false) private boolean active = true;
    @Version private long version;
    protected Ingredient() {}
    public Ingredient(String name, IngredientUnit unit, BigDecimal stockCurrent, BigDecimal stockMinimum, BigDecimal unitCost) { update(name, unit, stockMinimum, unitCost, true); this.stockCurrent = scale(stockCurrent); }
    public void update(String name, IngredientUnit unit, BigDecimal stockMinimum, BigDecimal unitCost, boolean active) { this.name = name.trim(); this.unit = unit; this.stockMinimum = scale(stockMinimum); this.unitCost = unitCost == null ? null : unitCost.setScale(4); this.active = active; }
    public void adjust(BigDecimal delta) { BigDecimal next = stockCurrent.add(scale(delta)); if (next.signum() < 0) throw new DomainException("Stock insuficiente para " + name); stockCurrent = next; }
    public boolean isLowStock() { return active && stockCurrent.signum() > 0 && stockCurrent.compareTo(stockMinimum) <= 0; }
    public boolean isOutOfStock() { return active && stockCurrent.signum() == 0; }
    private BigDecimal scale(BigDecimal value) { if (value == null) throw new DomainException("La cantidad es obligatoria"); return value.setScale(3); }
    public Long getId() { return id; } public String getName() { return name; } public IngredientUnit getUnit() { return unit; }
    public BigDecimal getStockCurrent() { return stockCurrent; } public BigDecimal getStockMinimum() { return stockMinimum; }
    public BigDecimal getUnitCost() { return unitCost; } public boolean isActive() { return active; }
}
